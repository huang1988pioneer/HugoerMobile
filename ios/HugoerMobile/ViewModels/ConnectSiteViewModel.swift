import Foundation

@MainActor
final class ConnectSiteViewModel: ObservableObject {
    @Published var repositoryInput: String = ""
    @Published var token: String = ""
    @Published var isVerifying = false
    @Published var errorMessage: String?
    @Published var verifiedLogin: String?
    @Published var verifiedRepo: GitHubRepoInfo?
    @Published var verifiedTarget: GitHubRepositoryTarget?

    var canVerify: Bool {
        !repositoryInput.trimmingCharacters(in: .whitespaces).isEmpty && !token.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var canSave: Bool {
        verifiedRepo != nil && verifiedTarget?.isValid == true
    }

    func verify() async {
        errorMessage = nil
        verifiedLogin = nil
        verifiedRepo = nil
        verifiedTarget = nil

        let target = GitHubRepositoryTarget.parse(repositoryInput)
        guard target.isValid else {
            errorMessage = target.errorMessage
            return
        }

        isVerifying = true
        defer { isVerifying = false }

        do {
            let login = try await GitHubApiService.getAuthenticatedUserLogin(token: token)
            guard login != nil else {
                errorMessage = "無法驗證權杖，請確認 PAT 是否正確且未過期。"
                return
            }

            guard let repoInfo = try await GitHubApiService.getRepoInfo(owner: target.owner, repo: target.repository, token: token) else {
                errorMessage = "找不到 repository，或權杖沒有存取權限。"
                return
            }

            verifiedLogin = login
            verifiedRepo = repoInfo
            verifiedTarget = target
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func buildProfile() -> SiteProfile? {
        guard let target = verifiedTarget, let repo = verifiedRepo else { return nil }
        return SiteProfile(
            owner: target.owner,
            repository: target.repository,
            displayName: "\(target.owner)/\(target.repository)",
            defaultBranch: repo.defaultBranch,
            pagesUrl: target.pagesUrl,
            isUserOrOrganizationSite: target.isUserOrOrganizationSite
        )
    }
}
