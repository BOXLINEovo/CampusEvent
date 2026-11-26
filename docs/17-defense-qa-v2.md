# CampusEvent 答辩问题清单与参考答案要点

> 使用方式：答案要点用于准备口述，不要求逐字背诵。
>
> 证据边界：涉及验收结果的问题，引用学生/教师提供的实际记录；本文不填写未提供的验收结论。

## Q1：为什么采用策略模式？具体解决了什么问题？

**要点**

- `RegistrationPolicy` 只负责两件事：初始状态和是否需要审批。
- `DirectRegistrationPolicy` 返回 `APPROVED`；`ReviewRegistrationPolicy` 返回 `PENDING`。
- `RegistrationService.register` 只依赖 `RegistrationPolicy` 与 `CapacityRule`，不写 `if (审核型)` 分支。
- 新增报名类型时，状态差异集中在策略实现；名额规则仍由 `CapacityRule` 独立处理。
- 本次年级限定没有新增 `PolicyType`，因为年级是“资格”而不是“初始状态”；资格规则放在 `ActivityGradeRuleV2`，初始状态仍是 `DIRECT` 的 `APPROVED`。

**关键代码**

- `domain/policy/RegistrationPolicy.java`
- `domain/policy/DirectRegistrationPolicy.java`
- `domain/policy/ReviewRegistrationPolicy.java`
- `application/service/RegistrationService.java` 的 `register` 方法

## Q2：既然有策略模式，为什么年级限定不新增一个策略？

**要点**

- 新增策略需要新增 `PolicyType` 枚举值和修改 `RegistrationPolicyFactory` 的 `switch`；本次约束要求原代码文件保持原样。
- 年级限定直接报名的初始状态仍是 `APPROVED`，与 `DIRECT` 相同；差异在“谁有资格报名”，属于资格规则，不属于初始状态策略。
- 因此采用“DIRECT + `activity_grade_rule_v2` + `GradeAwareRegistrationServiceV2` 装饰器”的组合。
- 代价：装饰器继承具体 `RegistrationService`，DIP 不够纯；收益：旧入口、旧页面、旧测试保持原样。
- 若允许修改原文件，优先把 `RegistrationService` 抽象为用例接口，再把年级资格做成可组合规则。

## Q3：`@Primary` 子类是如何工作的？有什么风险？

**要点**

- `GradeAwareRegistrationServiceV2` 继承 `RegistrationService`，标注 `@Primary`；Spring 在注入 `RegistrationService` 时优先选择它。
- 覆盖 `register`：先查年级规则；有规则时查学生年级；不符合抛出 `GradeRestrictionExceptionV2`；符合调用 `super.register`。
- 原 `RegistrationService` 仍在容器中，源码未改；旧控制器、旧 URL 自动使用新子类。
- 风险 1：`@Primary` 是隐式装配，维护者需要文档说明，否则容易误判。
- 风险 2：子类依赖父类具体实现，父类方法签名变化会影响子类。
- 风险 3：年级检查与 `super.register` 的行锁/名额检查分两次读取活动；并发窗口由原服务事务和行锁兜底。
- 关键代码：`application/service/GradeAwareRegistrationServiceV2.java`。

## Q4：为什么把年级放在独立表，而不是直接给 `Student` 加字段？

**要点**

- 约束要求原代码文件保持不变；独立 `student_grade_v2` 表让基线 `student` 表结构保持原样。
- 迁移采用纯新增表，回退成本低；已有学生通过 `INSERT ... SELECT` 默认年级 1，再补演示数据。
- 代价：`Student` 领域对象仍只有 id/name；年级通过 `StudentSummaryV2` 和 `StudentGradeRepositoryV2` 读取。
- 若课程要求物理列 `student.grade`，可以改成 `ALTER TABLE ... ADD COLUMN`；这属于数据库增量，不是代码文件修改。
- 关键文件：`V3__grade_and_email-v2.sql`、`JdbcStudentGradeRepositoryV2.java`。

## Q5：观察者/事件分发比直接在 `RegistrationService` 里发邮件好在哪里？

**要点**

- `RegistrationService` 只发布领域事件，不知道站内通知和邮件的存在；新增渠道时报名主流程保持原样。
- `EventDispatcher` 注入 `List<RegistrationEventObserver>`；新增 `EmailNotificationObserverV2` 后自动加入观察者列表。
- 原 `InSiteNotificationService`、`EventDispatcher`、`RegistrationEvent` 源码均未修改。
- outbox 把通知放在业务事务提交后派发；邮件失败记录错误，已提交报名结果保持不变。
- 代价：观察者运行在派发事务中，邮件观察者必须自己捕获异常；当前失败记录 `FAILED`，重试策略待决策。
- 关键代码：`EventDispatcher.dispatchPending`、`EmailNotificationObserverV2.onEvent`。

## Q6：如何保证邮件不重复发送？

**要点**

- 事件 ID 由 `RegistrationEvent` 生成；邮件表唯一键 `(source_event_id, student_id)`。
- 观察者发送前调用 `existsBySourceEventIdAndStudentId`；已存在则跳过。
- 数据库唯一键是并发/重试时的最后防线；适配器捕获 `DataIntegrityViolationException` 并转换为重复异常，观察者忽略重复。
- 当前是“至少一次投递 + 幂等记录”；严格一次需要“先占位 PENDING，再发送，再更新状态”的两阶段记录。
- 关键文件：`V3__grade_and_email-v2.sql`、`JpaEmailNotificationRepositoryV2.java`、`EmailNotificationObserverV2.java`。

## Q7：邮件发送失败时，为什么不会回滚报名？

**要点**

- 报名事务先提交，outbox 事件在提交后由 `EventDispatcher` 派发；邮件不在报名事务内。
- 邮件观察者 `try/catch` 发送异常，写入 `FAILED` 并记录日志，不向 `EventDispatcher` 抛出。
- 因此派发事务不因邮件失败而回滚，报名状态保持已提交结果；站内消息仍在同一派发事务中保存。
- 当前策略是记录失败并结束事件；如果要求自动重试，需要重新设计失败传播和事务边界。
- 关键代码：`EmailNotificationObserverV2.onEvent` 的 `try/catch` 与 `saveSafely`。

## Q8：为什么要用 outbox？直接 `@EventListener` 不行吗？

**要点**

- 课程项目明确采用“事务内 outbox + 提交后派发”，避免“数据库提交成功但通知丢失”或“通知已发但报名回滚”。
- `OutboxEventPublisher` 在业务事务内写 `outbox_event`；`EventDispatcher` 定时拉取并派发。
- 代价：最终一致、需要重试/幂等、派发有延迟（默认 5 秒）。
- 对课程规模，这是可解释、可验证的折中；消息队列超出本项目范围。
- 关键代码：`OutboxEventPublisher.publish`、`EventDispatcher.dispatchPending`。

## Q9：名额并发如何保证？年级检查会不会破坏原并发设计？

**要点**

- 原 `RegistrationService` 使用 `ActivityRepository.findByIdForUpdate` 悲观锁，register/approve/update/softDelete 共享活动行锁。
- 年级检查在 `super.register` 之前，只读规则和年级；真正插入与名额判断仍在原事务和行锁内。
- 因此年级检查不改变原名额并发语义；规则变更与报名之间的窗口由原服务的事务兜底。
- 若规则在报名瞬间被修改，可能出现“读取旧规则、按旧规则通过”的短暂窗口；当前管理端在有有效报名时拒绝修改范围，降低该风险。
- 关键代码：`RegistrationService.register`、`ActivityJpaRepository.findByIdForUpdate`、`GradeRuleAdminServiceV2.save`。

## Q10：如果活动已经有报名，再改年级范围怎么办？

**要点**

- `GradeRuleAdminServiceV2.save` 检测有效报名（已通过数 > 0 或存在待审核）。
- 年级范围发生变化且存在有效报名时，拒绝修改；相同范围重复保存保持幂等。
- 理由：避免已通过学生突然不符合新规则，造成历史数据与规则不一致。
- 这是一个产品决策；如果课程要求“只影响未来报名”，可以放开限制，并在报告中说明。
- 关键代码：`GradeRuleAdminServiceV2.hasActiveRegistrations`。

## Q11：为什么邮件收件人使用 `studentId@campus.example`？

**要点**

- 基线 `Student` 没有邮箱字段；课程要求使用 MockEmailSender，重点是记录收件人、主题、内容、状态、时间。
- 使用确定性模拟地址，便于把邮件记录与 `studentId` 对应；真实部署时替换 `EmailSenderV2` 适配器即可。
- 代价：收件人不是真实邮箱；若要求真实地址，需要新增学生邮箱字段和映射。
- 关键代码：`EmailNotificationObserverV2` 的 `recipient` 构造。

## Q12：项目中哪些设计使变更容易，哪些使变更困难？

**要点**

- 容易：观察者扩展点、端口/适配器、容量规则独立、`@Primary` 接管旧入口、纯新增表。
- 困难：`PolicyType` 枚举 + 工厂 `switch`、控制器依赖具体 `RegistrationService`、`Student` 无年级/仓储、`Activity` 无扩展字段、事件负载无邮箱、派发器单事务调用观察者。
- 结论：观察者/端口带来低回归扩展；枚举工厂与具体服务依赖带来耦合；本次用装饰器做约束下的折中。
- 证据：`git diff --name-only -- src/main src/test` 为空；新增 30 个类；原服务源码未改。

## Q13：如何证明没有破坏原功能？

**要点**

- 结构证据：`git diff --name-only -- src/main src/test` 为空，原 Java/测试文件无修改；v1 UML 源文件与导出图无 diff。
- 测试证据：原有 5 个测试类/16 个方法保留；新增 3 个测试类/10 个方法；本地 `mvn test` 记录 26 个测试、0 失败、0 错误（构建阶段证据）。
- 冒烟证据：旧 `/activities/{id}/registrations` 直接报名路径可用；无规则活动走 `super.register`。
- 验收边界：教师验收结果未提供；报告中不填写“验收通过”，只列测试/构建证据。
- 建议学生补充：实际验收截图、教师签字、回归执行记录。

## Q14：为什么 Java 文件用 `V2` 后缀而不是 `-v2`？

**要点**

- Java 公共类文件名必须与类名一致，类名不能包含 `-`；`GradeAwareRegistrationServiceV2.java` 合法，`...-v2.java` 无法承载 public class。
- 非 Java 文件严格使用 `-v2`：模板、SQL、UML、文档。
- 这是语言约束与命名要求的冲突；报告中明确说明，避免被误认为违反约定。
- 如果课程要求所有文件字面以 `-v2` 结尾，需要改为包内非 public 类或调整约束；当前采用可编译的 `V2` 后缀。

## Q15：如果继续迭代，你会怎么改？

**要点**

- 抽取 `RegistrationUseCase` 接口，去掉 `@Primary` 具体继承；把年级资格做成可组合规则。
- 增加真实数据库集成测试（独立测试库）覆盖 Flyway、唯一约束、outbox + 观察者。
- 增加邮件重试/两阶段占位，明确“至少一次”与“严格一次”的边界。
- 把学生邮箱、真实邮件适配器、管理端权限（Nginx Basic Auth/IP 白名单）补齐。
- 统一 v2 UML 全量图，补部署与验收证据。
- 关键取舍：先保持低回归，再逐步抽象。

## 答辩表达建议

- 先讲约束，再讲取舍：为什么没有改原文件、为什么用 `@Primary`、为什么用扩展表。
- 用文件/行号/命令作证据，避免只说“用了策略模式/观察者模式”。
- 对未提供的验收结果明确说“由学生/教师补充”，不推断通过。
- 主动说明方案 A 的 DIP 折中，以及方案 B 的旧入口缺口，体现分析深度。
