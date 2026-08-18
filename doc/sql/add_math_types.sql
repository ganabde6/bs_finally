-- 补充数学学科(subject_id=2)的多选题/填空题/简答题/计算题
-- 目的:支持"单选+多选+填空+答题(简答/计算)"题型组合的 AI 智能组卷
USE zhixue_ai;

-- ============ 多选题 (question_type=2) ============
INSERT INTO exam_question (subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, analysis, full_score, deleted) VALUES
(2, 2, 2, '函数', '下列函数中,在定义域内为增函数的有:',
 '[{"key":"A","value":"y=2x+1"},{"key":"B","value":"y=x²(x≥0)"},{"key":"C","value":"y=-x"},{"key":"D","value":"y=1/x(x>0)"}]',
 'AB', 'A为一次函数斜率2>0递增;B在x≥0上递增;C递减;D在(0,+∞)递减', 3.00, 0),

(2, 2, 2, '不等式', '下列不等式中,解集为全体实数的有:',
 '[{"key":"A","value":"x²+1>0"},{"key":"B","value":"x²≥0"},{"key":"C","value":"x²+2x+3>0"},{"key":"D","value":"x²<0"}]',
 'ABC', 'A、B、C恒成立;D无解', 3.00, 0),

(2, 2, 3, '三角函数', '下列三角函数值正确的有:',
 '[{"key":"A","value":"sin45°=√2/2"},{"key":"B","value":"cos60°=1/2"},{"key":"C","value":"tan30°=√3"},{"key":"D","value":"sin90°=1"}]',
 'ABD', 'C错误:tan30°=√3/3', 3.00, 0),

(2, 2, 2, '一元二次方程', '下列方程中,有两个相等实数根的有:',
 '[{"key":"A","value":"x²-2x+1=0"},{"key":"B","value":"x²-4=0"},{"key":"C","value":"x²+2x+1=0"},{"key":"D","value":"x²+1=0"}]',
 'AC', 'A、C的判别式Δ=0;D无实数根;D应排除,判别式Δ<0', 3.00, 0),

(2, 2, 2, '集合', '下列集合关系中,正确的有:',
 '[{"key":"A","value":"∅⊆{1,2}"},{"key":"B","value":"{1}∈{1,2}"},{"key":"C","value":"{1}⊂{1,2}"},{"key":"D","value":"{1,2}={2,1}"}]',
 'ACD', 'B错误:应用⊆而非∈', 3.00, 0);

-- ============ 填空题 (question_type=4) ============
INSERT INTO exam_question (subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, analysis, full_score, deleted) VALUES
(2, 4, 1, '函数', '函数 y=2x+1 中,当 x=3 时,y=____。', NULL, '7', 'y=2×3+1=7', 2.00, 0),
(2, 4, 2, '函数', '二次函数 y=x²-2x 的顶点横坐标为____。', NULL, '1', '顶点横坐标 x=-b/2a=2/2=1', 2.00, 0),
(2, 4, 2, '不等式', '不等式 3x-6≤0 的解集为 x____。', NULL, 'x≤2', '移项得 3x≤6,除以3得 x≤2', 2.00, 0),
(2, 4, 3, '三角函数', '已知 sinα=1/2 且 α 为锐角,则 α=____°。', NULL, '30', '特殊角三角函数值', 2.00, 0),
(2, 4, 1, '集合', '集合 A={1,2,3},B={2,3,4},则 A∪B 中有____个元素。', NULL, '4', 'A∪B={1,2,3,4}', 2.00, 0);

-- ============ 简答题 (question_type=5) ============
INSERT INTO exam_question (subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, analysis, full_score, deleted) VALUES
(2, 5, 2, '函数', '简述一次函数 y=kx+b(k≠0) 图像经过的象限与系数 k、b 的关系。', NULL, '当k>0,b>0时经过一、二、三象限;当k>0,b<0时经过一、三、四象限;当k<0,b>0时经过一、二、四象限;当k<0,b<0时经过二、三、四象限。', '按k与b的正负分四种情况讨论', 5.00, 0),
(2, 5, 3, '不等式', '解不等式组并写出整数解:2x-1>3 且 x-2≤0。', NULL, '由2x-1>3得x>2;由x-2≤0得x≤2;不等式组无解,不存在整数解。', '分别解两个不等式,再求交集', 5.00, 0),
(2, 5, 2, '三角函数', '简述在直角三角形中,锐角α的正弦、余弦、正切的定义。', NULL, 'sinα=对边/斜边;cosα=邻边/斜边;tanα=对边/邻边。', '三角函数的直角三角形定义', 5.00, 0);

-- ============ 计算题 (question_type=7) ============
INSERT INTO exam_question (subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, analysis, full_score, deleted) VALUES
(2, 7, 2, '函数', '已知一次函数 y=kx+b 的图像经过点 A(1,3) 和 B(-1,-1),求 k 和 b 的值。', NULL, '代入得:k+b=3,-k+b=-1,两式相加得2b=2,即b=1;代入得k=2。所以k=2,b=1。', '待定系数法解二元一次方程组', 5.00, 0),
(2, 7, 3, '一元二次方程', '用公式法解方程 2x²-3x-2=0。', NULL, 'a=2,b=-3,c=-2,Δ=b²-4ac=9+16=25,√Δ=5,x=(3±5)/4,所以x₁=2,x₂=-1/2。', '一元二次方程求根公式', 5.00, 0),
(2, 7, 3, '函数', '求二次函数 y=x²-4x+3 的对称轴、顶点坐标,并判断其最值。', NULL, '对称轴x=-b/2a=2;顶点纵坐标=4-8+3=-1,顶点为(2,-1);因a=1>0,开口向上,有最小值-1,无最大值。', '配方法或顶点公式', 5.00, 0),
(2, 7, 2, '导数', '求函数 f(x)=x³-3x 在 x=1 处的导数值。', NULL, 'f''(x)=3x²-3,f''(1)=3-3=0,所以导数值为0。', '幂函数求导后代入', 5.00, 0);
