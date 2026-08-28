import Foundation

@MainActor
final class GitHubViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isEnablingPages = false
    @Published var isDeploying = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    @Published var repoInfo: GitHubRepoInfo?
    @Published var pagesStatus: GitHubPagesStatus?
    @Published var workflowRuns: [WorkflowRunInfo] = []

    private var site: SiteProfile?
    private var token: String?

    func load(site: SiteProfile, token: String) async {
        self.site = site
        self.token = token
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        async let repoTask = try? GitHubApiService.getRepoInfo(owner: site.owner, repo: site.repository, token: token)
        async let pagesTask = try? GitHubApiService.getPagesStatus(owner: site.owner, repo: site.repository, token: token)
        async let runsTask = try? GitHubApiService.listWorkflowRuns(owner: site.owner, repo: site.repository, token: token, take: 10)

        repoInfo = await repoTask
        pagesStatus = await pagesTask
        workflowRuns = await runsTask ?? []
    }

    func enablePages() async {
        guard let site, let token else { return }
        isEnablingPages = true
        errorMessage = nil
        successMessage = nil
        defer { isEnablingPages = false }

        do {
            let result = try await GitHubApiService.enablePagesFromActions(owner: site.owner, repo: site.repository, token: token)
            if result.success {
                successMessage = result.message
                pagesStatus = try await GitHubApiService.getPagesStatus(owner: site.owner, repo: site.repository, token: token)
            } else {
                errorMessage = result.message
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deployNow() async {
        guard let site, let token else { return }
        isDeploying = true
        errorMessage = nil
        successMessage = nil
        defer { isDeploying = false }

        do {
            try await GitHubApiService.ensureDeployWorkflow(owner: site.owner, repo: site.repository, branch: site.defaultBranch, token: token)

            let workflows = try await GitHubApiService.listWorkflows(owner: site.owner, repo: site.repository, token: token)
            guard let workflow = workflows.first(where: { $0.path.hasSuffix("hugo.yml") }) ?? workflows.first else {
                errorMessage = "找不到可觸發的 GitHub Actions workflow。"
                return
            }

            try await GitHubApiService.dispatchWorkflow(owner: site.owner, repo: site.repository, workflowId: workflow.id, branch: site.defaultBranch, token: token)
            successMessage = "已觸發部署，請稍後查看下方執行紀錄。"

            // Give GitHub a brief moment to register the new run before refreshing.
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            workflowRuns = (try? await GitHubApiService.listWorkflowRuns(owner: site.owner, repo: site.repository, token: token, take: 10)) ?? []
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
