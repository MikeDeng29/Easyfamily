import XCTest

final class EasyfamilyAppUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    /// Logs in via "一键登录（测试）" and lands on the Chat tab.
    private func quickLogin(_ app: XCUIApplication) {
        let quickLoginButton = app.buttons["一键登录（测试）"]
        XCTAssertTrue(quickLoginButton.waitForExistence(timeout: 10))
        quickLoginButton.tap()

        // Quick login performs captcha + SMS + login over the network; allow time for it.
        let chatTab = app.tabBars.buttons["对话"]
        XCTAssertTrue(chatTab.waitForExistence(timeout: 20), "未能在一键登录后进入主界面")
    }

    func testQuickLoginShowsChatHome() {
        let app = XCUIApplication()
        app.launch()

        quickLogin(app)

        XCTAssertTrue(app.staticTexts["🏠 AI 智能助手"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.tabBars.buttons["我的"].exists)
    }

    func testSendChatMessageReceivesAiReply() {
        let app = XCUIApplication()
        app.launch()

        quickLogin(app)

        let input = app.textFields["输入或说出你的问题..."]
        XCTAssertTrue(input.waitForExistence(timeout: 10))
        input.tap()
        input.typeText("你好")

        let sendButton = app.images["arrow.up.circle.fill"]
        XCTAssertTrue(sendButton.exists)
        sendButton.tap()

        // AI reply bubble shows a 🤖 prefix; wait for the streamed response to start/finish.
        let aiBubble = app.staticTexts["🤖"]
        XCTAssertTrue(aiBubble.waitForExistence(timeout: 30), "未收到 AI 回复")
    }

    func testMineDestinationsNavigate() {
        let app = XCUIApplication()
        app.launch()

        quickLogin(app)

        app.tabBars.buttons["我的"].tap()
        XCTAssertTrue(app.navigationBars["我的"].waitForExistence(timeout: 10))

        let destinations: [(label: String, title: String)] = [
            ("大家庭", "大家庭"),
            ("手机号", "手机号"),
            ("查询", "查询"),
            ("车辆", "车辆"),
            ("账单", "账单")
        ]

        for destination in destinations {
            let cell = app.staticTexts[destination.label]
            XCTAssertTrue(cell.waitForExistence(timeout: 10), "未找到入口：\(destination.label)")
            cell.tap()

            XCTAssertTrue(app.navigationBars[destination.title].waitForExistence(timeout: 10), "未能进入：\(destination.title)")

            app.navigationBars.buttons.element(boundBy: 0).tap()
            XCTAssertTrue(app.navigationBars["我的"].waitForExistence(timeout: 10))
        }
    }

    func testLogout() {
        let app = XCUIApplication()
        app.launch()

        quickLogin(app)

        app.tabBars.buttons["我的"].tap()
        let logoutButton = app.staticTexts["退出登录"]
        XCTAssertTrue(logoutButton.waitForExistence(timeout: 10))
        logoutButton.tap()

        XCTAssertTrue(app.buttons["一键登录（测试）"].waitForExistence(timeout: 10), "退出登录后未返回登录页")
    }
}
