-- 测试数据：跑腿任务 / 题库 / 学习资源 —— 论文第 5 章测试截图用
-- 用法：Navicat 里对 campus_service 库执行本文件。重复执行会重复插入，先确保没跑过。
-- 依赖：user 表已有 userId 2 / 10 / 12（跑腿发布者与接单者）

-- ============ 1. 跑腿任务（任务广场）============
INSERT INTO `task`
(`publisher_id`,`taker_id`,`pickup_location`,`pickup_lat`,`pickup_lng`,
 `delivery_location`,`delivery_lat`,`delivery_lng`,`fee`,`deadline`,`remark`,
 `task_type`,`status`,`create_time`,`take_time`,`complete_time`) VALUES
(10, NULL, '九江学院 主校区菜鸟驿站',      29.705100,115.992300, '男生宿舍 7 栋 502',      29.708200,115.995100, 4.50, DATE_ADD(NOW(), INTERVAL 1 DAY), '一个中等大小的纸箱，麻烦轻拿',       '取快递', 0, NOW(), NULL, NULL),
(10, NULL, '学校一食堂 二楼窗口',          29.706000,115.993500, '图书馆 3 楼自习区',      29.707400,115.994600, 6.00, DATE_ADD(NOW(), INTERVAL 6 HOUR), '一份麻辣香锅，不要香菜，多加米饭', '取外卖', 0, NOW(), NULL, NULL),
(12, NULL, '校园超市（西门口）',            29.704200,115.991000, '教学楼 A 座 305',        29.705500,115.992800, 3.00, DATE_ADD(NOW(), INTERVAL 3 HOUR), '一瓶 1.5L 矿泉水 + 一包抽纸',      '代买', 0, NOW(), NULL, NULL),
(12, NULL, '宿舍楼 3 栋快递柜',             29.709000,115.996200, '实验楼 B 座 208',        29.706600,115.993900, 5.00, DATE_ADD(NOW(), INTERVAL 2 DAY), '实验器材配件，注意防摔',           '取快递', 0, NOW(), NULL, NULL),
(10, NULL, '图书馆 一楼借还书处',           29.707200,115.994400, '女生宿舍 2 栋 301',      29.708800,115.995700, 4.00, DATE_ADD(NOW(), INTERVAL 1 DAY), '4 本专业书，比较重',               '代办', 0, NOW(), NULL, NULL),
(12, 10,   '校外 沃尔玛超市',               29.701000,115.988000, '男生宿舍 7 栋 502',      29.708200,115.995100, 8.00, DATE_ADD(NOW(), INTERVAL 8 HOUR), '已和小哥说好，帮忙跑一趟',         '代买', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL),
(10, 12,   '快递点（东门）',                29.703400,115.990200, '宿舍楼 5 栋 108',        29.708600,115.995400, 5.00, DATE_SUB(NOW(), INTERVAL 2 HOUR), '已送达，谢谢',                     '取快递', 2, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- ============ 2. 题库（客观题可自动判分，chapter 用于错题本反向推荐）============
INSERT INTO `question`
(`subject`,`chapter`,`q_type`,`content`,`options`,`answer`,`analysis`,`create_time`) VALUES
('数据结构','线性表',0,'在一个长度为 n 的顺序表中，在第 i 个位置插入一个元素，平均需要移动多少个元素？',
 '["A. n","B. n/2","C. (n-i)","D. n-i+1"]','B','插入位置等概率时，平均移动次数为 n/2，时间复杂度 O(n)。',NOW()),
('数据结构','线性表',2,'单链表可以随机访问任意一个节点。','[]','错误','单链表只能从头结点顺序查找，访问第 k 个元素需要 O(n) 时间。',NOW()),
('数据结构','栈和队列',0,'下列哪种结构适合实现“后进先出”的访问顺序？',
 '["A. 队列","B. 栈","C. 数组","D. 链表"]','B','栈是后进先出（LIFO）结构，队列是先进先出（FIFO）。',NOW()),
('数据结构','栈和队列',3,'循环队列中，判断队满通常需要牺牲一个存储单元，其判满条件是 front == (rear+1) % _____。',
 '[]','maxSize','循环队列用“留一个空位”区分队空与队满。',NOW()),
('数据结构','树与二叉树',0,'一棵有 100 个结点的完全二叉树，其叶子结点的个数是？',
 '["A. 49","B. 50","C. 51","D. 52"]','B','完全二叉树 n=100 时叶子数为 ⌈n/2⌉ = 50。',NOW()),
('数据结构','树与二叉树',1,'下列遍历方式中，属于深度优先遍历的有哪些？',
 '["A. 先序遍历","B. 中序遍历","C. 层序遍历","D. 后序遍历"]','ABD','层序遍历属于广度优先，其余三种是深度优先。',NOW()),
('数据结构','图',0,'用邻接矩阵存储含 n 个顶点的图，其空间复杂度为？',
 '["A. O(n)","B. O(n²)","C. O(n+e)","D. O(e)"]','B','邻接矩阵需要 n×n 的空间，即 O(n²)。',NOW()),
('数据结构','排序',0,'下列排序算法中，平均时间复杂度为 O(n log n) 且不稳定的是？',
 '["A. 冒泡排序","B. 归并排序","C. 快速排序","D. 插入排序"]','C','快排平均 O(n log n) 但不稳定；归并排序稳定。',NOW()),
('数据结构','排序',2,'快速排序在最坏情况下的时间复杂度是 O(n log n)。','[]','错误','快排最坏（如已有序且每次取首元素为枢轴）为 O(n²)。',NOW()),
('英语四级','词汇',0,'The word "abandon" most nearly means ____.',
 '["A. to keep","B. to give up","C. to repair","D. to borrow"]','B','abandon 意为“放弃、抛弃”。',NOW()),
('英语四级','阅读理解',3,'According to the passage, the main reason for the decline is the lack of ____.',
 '[]','funding','原文第三段指出资金不足是主因。',NOW()),
('考研数学','高等数学',0,'当 x→0 时，sin x 与 x 是____。',
 '["A. 高阶无穷小","B. 等价无穷小","C. 低阶无穷小","D. 无关"]','B','lim(sin x / x) = 1，故为等价无穷小。',NOW()),
('考研数学','线性代数',0,'设 A 为 3 阶方阵且 |A| = 2，则 |2A| = ？',
 '["A. 4","B. 8","C. 16","D. 6"]','C','|kA| = kⁿ|A|，故 |2A| = 2³ × 2 = 16。',NOW()),
('考研数学','概率论',4,'简述大数定律的直观含义。','[]','样本量越大，样本均值依概率收敛于总体均值。','大数定律说明“频率稳定于概率”，是统计推断的基础。',NOW());

-- ============ 3. 学习资源（status=1 已通过，才会出现在列表里）============
INSERT INTO `resource`
(`user_id`,`title`,`description`,`course`,`resource_type`,`file_name`,`file_url`,`file_ext`,
 `file_size`,`view_count`,`download_count`,`collect_count`,`score`,`status`,`reject_reason`,
 `create_time`,`audit_time`) VALUES
(10,'数据结构 期末复习重点整理','按考纲整理了线性表/树/图/排序四章的重点与常考题型','数据结构','笔记','数据结构复习重点.pdf',
 'https://bishe-ai-version.oss-cn-beijing.aliyuncs.com/campus/demo/ds-review.pdf','pdf',2048000,132,41,18,4.8,1,NULL,DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY)),
(10,'数据结构 实验课代码合集','链表/栈/队列/二叉树/图的完整实现代码，含注释','数据结构','代码','ds-lab-code.zip',
 'https://bishe-ai-version.oss-cn-beijing.aliyuncs.com/campus/demo/ds-lab.zip','zip',512000,88,26,11,4.6,1,NULL,DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_SUB(NOW(), INTERVAL 7 DAY)),
(12,'英语四级 高频词汇 500 词','按词频排序，每词附例句','英语四级','笔记','四级核心词汇500.pdf',
 'https://bishe-ai-version.oss-cn-beijing.aliyuncs.com/campus/demo/cet4-500.pdf','pdf',1536000,215,73,39,4.9,1,NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(12,'高等数学 历年真题（含答案）','近五年期末真题 + 详细解答','高等数学','真题','高数真题合集.pdf',
 'https://bishe-ai-version.oss-cn-beijing.aliyuncs.com/campus/demo/math-exam.pdf','pdf',4096000,301,124,52,4.7,1,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10,'计算机网络 课件（第 1-6 章）','老师上课用的 PPT 原版','计算机网络','课件','计网课件1-6章.pptx',
 'https://bishe-ai-version.oss-cn-beijing.aliyuncs.com/campus/demo/net-ppt.pptx','pptx',8192000,64,19,7,4.5,1,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(12,'操作系统 期末复习提纲','含 PV 操作、页面置换、死锁等高频考点','操作系统','笔记','操作系统提纲.pdf',
 'https://bishe-ai-version.oss-cn-beijing.aliyuncs.com/campus/demo/os-outline.pdf','pdf',1024000,47,12,5,0.0,0,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NULL);

-- ============ 4. 求资源（需求匹配：求资源 ↔ 有资源）============
INSERT INTO `resource_demand`
(`user_id`,`title`,`course`,`resource_type`,`keyword`,`description`,`status`,`match_count`,`create_time`) VALUES
(12,'求《数据结构》期末复习课件','数据结构','课件','数据结构 复习','想要老师课件原版，谢谢！',0,2,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10,'求英语四级真题（近三年）','英语四级','真题','四级 真题','只要真题，最好带听力原文',0,1,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12,'求《操作系统》实验报告模板','操作系统','代办','操作系统 实验报告','有模板的同学求分享一份',1,0,DATE_SUB(NOW(), INTERVAL 5 DAY));
