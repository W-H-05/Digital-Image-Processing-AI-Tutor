-- ============================================
-- 基础数据初始化
-- 注意：教师/学生账号由应用启动时 DataInitializer 用 BCrypt 编码创建
-- ============================================
USE ai_tutor;

-- 课程
INSERT INTO `course` (`id`, `course_name`, `description`, `teacher_id`) VALUES
(1, '数字图像处理', '《数字图像处理》课程，涵盖图像基础、空间域处理、频域处理、形态学、分割、特征提取、视频处理等内容。', NULL);

-- 课次包（第1-24次课 + 实验1-4）
INSERT INTO `lesson_pack` (`id`, `course_id`, `lesson_no`, `title`, `chapter`, `status`, `objectives`, `key_points`, `difficult_points`) VALUES
(1, 1, '第1次课', '图像基础与灰度直方图', '图像处理绪论', 'PUBLISHED', '了解数字图像基本概念；掌握灰度直方图及其意义。', '采样、量化、灰度直方图、对比度', '采样量化与分辨率的关系'),
(2, 1, '第2次课', '点运算与直方图均衡化', '空间域增强', 'PUBLISHED', '掌握直方图均衡化、直方图规定化原理。', '直方图均衡化、对比度拉伸、γ校正', '均衡化的数学推导'),
(3, 1, '第3次课', '空间滤波：平滑与锐化', '空间域增强', 'PUBLISHED', '理解卷积；掌握均值/高斯/中值滤波与锐化算子。', '卷积、高斯滤波、中值滤波、Sobel、拉普拉斯', '卷积边界处理与模板归一化'),
(4, 1, '第4次课', '傅里叶变换与频域滤波', '频域处理', 'PUBLISHED', '理解二维 DFT；掌握低通/高通滤波。', 'DFT、频谱、理想/高斯低通高通', '频谱搬移与频域滤波流程'),
(5, 1, '第5次课', '形态学处理', '形态学', 'PUBLISHED', '掌握腐蚀、膨胀、开闭运算及其应用。', '腐蚀、膨胀、开运算、闭运算、结构元素', '开闭运算的先后顺序与效果'),
(6, 1, '第6次课', '图像分割：阈值与边缘', '分割', 'PUBLISHED', '掌握 Otsu、Canny 边缘检测。', 'Otsu、Canny、区域生长', 'Canny 双阈值与非极大值抑制'),
(7, 1, '第7次课', '特征提取与描述', '特征', 'PUBLISHED', '理解 LBP、SIFT 特征。', 'LBP、SIFT、HOG', 'SIFT 尺度空间与关键点定位'),
(8, 1, '第8次课', '视频处理与目标检测', '视频处理', 'PUBLISHED', '了解帧差法、背景建模 MOG2。', '帧差法、MOG2、光流', '背景建模的更新策略'),
(9, 1, '实验1', '图像基本操作实验', '实验', 'PUBLISHED', 'OpenCV 读写图像、颜色空间转换。', 'imread、cvtColor、BGR/RGB', 'BGR 与 RGB 通道顺序'),
(10, 1, '实验2', '直方图与滤波实验', '实验', 'PUBLISHED', '绘制直方图、实现滤波对比。', 'calcHist、GaussianBlur、medianBlur', '核大小选择'),
(11, 1, '实验3', '形态学与分割实验', '实验', 'PUBLISHED', '形态学操作与 Otsu/Canny 分割。', 'morphologyEx、threshold、Canny', '结构元素形状选择'),
(12, 1, '实验4', '特征提取实验', '实验', 'PUBLISHED', 'LBP/SIFT 特征提取与匹配。', 'SIFT、LBP、BFMatcher', '特征点数量与匹配阈值');

-- 知识点
INSERT INTO `knowledge_point` (`id`, `lesson_pack_id`, `name`, `description`, `keywords`) VALUES
(1, 1, '采样与量化', '连续图像离散化为像素矩阵的过程；采样决定空间分辨率，量化决定灰度分辨率。', '采样,量化,分辨率,像素'),
(2, 1, '灰度直方图', '统计图像各灰度级出现频率，反映图像亮度分布与对比度。', '直方图,灰度级,对比度,频率'),
(3, 2, '直方图均衡化', '通过灰度映射使直方图近似均匀分布，增强对比度。', '均衡化,累计分布,CDF,对比度'),
(4, 3, '空间滤波', '用模板与图像做卷积实现平滑或锐化。', '卷积,模板,核,平滑,锐化'),
(5, 4, '频域滤波', '对图像做 DFT 后在频域乘以滤波器再逆变换。', 'DFT,频谱,低通,高通,FFT'),
(6, 5, '形态学', '基于结构元素的腐蚀膨胀及开闭运算。', '腐蚀,膨胀,开运算,闭运算,结构元素'),
(7, 6, 'Otsu 阈值分割', '最大化类间方差的自动阈值选取。', 'Otsu,阈值,类间方差,二值化'),
(8, 6, 'Canny 边缘检测', '高斯平滑+梯度+非极大值抑制+双阈值+滞后连接。', 'Canny,边缘,梯度,双阈值'),
(9, 7, 'SIFT 特征', '尺度不变特征变换，检测并描述关键点。', 'SIFT,关键点,尺度空间,描述子'),
(10, 8, '背景建模', '用 MOG2 等方法估计背景实现运动目标检测。', 'MOG2,背景建模,帧差,运动检测');

-- 交互课件
INSERT INTO `courseware` (`id`, `lesson_pack_id`, `name`, `type`, `config_json`) VALUES
(1, 1, '灰度直方图实验室', '直方图', '{"min":0,"max":255,"checkpoint":"观察不同亮度图像的直方图形态"}'),
(2, 2, '直方图均衡化实验室', '均衡化', '{"gamma":[0.3,3.0],"checkpoint":"拖动γ观察对比度变化"}'),
(3, 3, '低通滤波实验室', '低通', '{"kernel":[3,5,7,9,11],"checkpoint":"比较均值与高斯滤波的模糊效果"}'),
(4, 4, '频域滤波实验室', '频域', '{"radius":[10,200],"checkpoint":"调整截止频率观察高低通效果"}'),
(5, 5, '形态学实验室', '形态学', '{"op":["腐蚀","膨胀","开运算","闭运算"],"kernelSize":[3,5,7],"checkpoint":"对比开闭运算对噪声的抑制"}'),
(6, 6, 'Otsu 分割实验室', '分割', '{"threshold":[0,255],"checkpoint":"对比手动阈值与Otsu自动阈值"}'),
(7, 7, 'Canny 边缘实验室', '边缘', '{"low":[0,255],"high":[0,255],"checkpoint":"调整双阈值观察边缘连续性"}'),
(8, 8, '视频目标检测实验室', '视频', '{"method":["帧差法","MOG2"],"checkpoint":"观察运动目标的检测框"}');

-- 示例材料（第1次课）
INSERT INTO `lesson_material` (`lesson_pack_id`, `material_type`, `title`, `description`, `group_name`, `is_open_to_student`, `sort_index`) VALUES
(1, '教案', '第1次课教案', '教学目标、过程、思政、反思（默认不开放）', '课前预习', 0, 0),
(1, '重难点', '采样与量化·重点难点', '采样决定空间分辨率，量化决定灰度分辨率；易混淆分辨率与清晰度。', '课前预习', 1, 1),
(1, '知识点', '灰度直方图讲解', '直方图反映图像亮度分布；暗图直方图偏左，亮图偏右，低对比度图直方图集中。', '课中讲解', 1, 2),
(1, '作业', '第1次课作业', '编写 Python 程序统计并绘制给定图像的灰度直方图。', '课后作业', 1, 3);

-- RAG 知识库：课程核心概念
INSERT INTO `rag_document` (`title`, `category`, `content`, `chunk_index`) VALUES
('灰度直方图概念', '教材', '灰度直方图是图像灰度级的统计函数，横轴为灰度级(0~255)，纵轴为该灰度级出现的像素个数或频率。它反映图像的亮度分布和对比度：偏暗的图像直方图集中在低灰度区，偏亮的集中在高灰度区，低对比度图像直方图集中在一个窄区间，高对比度图像直方图分布较均匀。', 0),
('直方图均衡化原理', '教材', '直方图均衡化通过灰度变换函数 s=T(r) 将原图灰度级映射，使输出图像直方图近似均匀分布。T(r) 取输入灰度级的累计分布函数(CDF)。其作用是自动增强图像对比度，尤其适用于背景与前景都较亮或较暗的图像。局限是可能放大噪声、丢失灰度层次。', 0),
('采样与量化', '教材', '采样是对空间坐标的离散化，决定图像的空间分辨率（像素数）；量化是对灰度值的离散化，决定灰度级分辨率（如8位=256级）。采样间隔越小、量化级数越多，图像越精细，但数据量越大。欠采样会产生混叠（马赛克/锯齿），欠量化会产生伪轮廓。', 0),
('傅里叶变换与频谱', '教材', '二维离散傅里叶变换(DFT)将图像从空间域变换到频率域。频谱中心低频对应图像中变化平缓的区域（大面积背景），高频对应边缘、纹理和噪声等剧烈变化。频率域滤波即对频谱乘以滤波器函数：低通滤波保留低频、模糊图像；高通滤波保留高频、突出边缘。', 0),
('形态学腐蚀与膨胀', '教材', '腐蚀使目标边界向内收缩，可消除小于结构元素的噪声和细连接；膨胀使目标边界向外扩张，可填补空洞、连接断裂。开运算是先腐蚀后膨胀，平滑轮廓并消除小物体；闭运算是先膨胀后腐蚀，填充小孔并连接邻近物体。', 0),
('Otsu 阈值分割', '教材', 'Otsu 方法通过最大化前景与背景的类间方差来自动选取最佳阈值。它遍历所有灰度级，计算每个阈值对应的类间方差，取方差最大者为阈值。适用于灰度直方图呈双峰分布的图像，计算简单且稳定，是最常用的自动全局阈值方法。', 0),
('Canny 边缘检测', '教材', 'Canny 算法步骤：1) 高斯滤波平滑；2) 计算梯度幅值和方向；3) 非极大值抑制细化边缘；4) 双阈值(高低阈值)检测强弱边缘；5) 滞后连接弱边缘到强边缘。双阈值中高于高阈值的为强边缘，低于低阈值的舍弃，介于之间的若与强边缘连接则保留。', 0),
('SIFT 特征', '教材', 'SIFT(尺度不变特征变换)通过构建高斯差分金字塔在不同尺度空间检测极值点作为关键点，为每个关键点计算梯度方向直方图生成128维描述子，具有尺度、旋转、光照不变性，广泛用于图像匹配与拼接。', 0),
('LBP 纹理特征', '教材', '局部二值模式(LBP)以中心像素为阈值，将邻域像素与中心比较，大于为1否则为0，得到二进制编码作为该点纹理描述。LBP 对灰度单调变化鲁棒、计算快，广泛用于纹理分类和人脸识别。', 0),
('MOG2 背景建模', '教材', 'MOG2 使用混合高斯模型对每个像素建模，动态更新均值和方差，将长时间稳定的像素判为背景，从而实现运动目标检测。相比帧差法，它更能适应光照变化和缓慢移动的背景，是 OpenCV 中 createBackgroundSubtractorMOG2 的实现。', 0);

-- RAG 知识库：OpenCV 函数
INSERT INTO `rag_document` (`title`, `category`, `content`, `chunk_index`) VALUES
('cv2.imread', 'OpenCV', 'cv2.imread(path, flags) 读取图像，flags 常用 cv2.IMREAD_COLOR(默认，彩色3通道BGR)、cv2.IMREAD_GRAYSCALE(灰度)、cv2.IMREAD_UNCHANGED(含alpha)。注意：OpenCV 读取的是 BGR 顺序而非 RGB；路径含中文或找不到文件会返回 None，需判空。', 0),
('cv2.cvtColor', 'OpenCV', 'cv2.cvtColor(src, code) 颜色空间转换，常用 code：cv2.COLOR_BGR2RGB、cv2.COLOR_BGR2GRAY、cv2.COLOR_BGR2HSV。将 OpenCV 的 BGR 图像转 RGB 用于 matplotlib 显示时必须用 BGR2RGB，否则颜色偏蓝。', 0),
('cv2.GaussianBlur', 'OpenCV', 'cv2.GaussianBlur(src, ksize, sigmaX) 高斯滤波，ksize 为核大小(奇数如(5,5))，sigmaX 为标准差，设 0 时由 ksize 自动计算。核越大平滑越强；用于去噪和作为 Canny 前置平滑。', 0),
('cv2.medianBlur', 'OpenCV', 'cv2.medianBlur(src, ksize) 中值滤波，ksize 为奇数。对椒盐噪声效果好，能保留边缘；但核越大计算越慢。', 0),
('cv2.equalizeHist', 'OpenCV', 'cv2.equalizeHist(src) 直方图均衡化，仅支持单通道灰度图；彩色图需先分离通道或转 YCrCb 对亮度通道均衡。', 0),
('cv2.calcHist', 'OpenCV', 'cv2.calcHist([img], [0], None, [256], [0,256]) 计算直方图，参数依次为：图像列表、通道索引、掩膜、histSize(灰度级数)、范围。返回数组可直接用于绘制直方图。', 0),
('cv2.morphologyEx', 'OpenCV', 'cv2.morphologyEx(src, op, kernel) 形态学操作，op 常用 cv2.MORPH_OPEN(开运算)、cv2.MORPH_CLOSE(闭运算)、cv2.MORPH_GRADIENT(梯度)、cv2.MORPH_TOPHAT。kernel 用 cv2.getStructuringElement(shape, ksize) 生成，如 MORPH_RECT/ELLIPSE/CROSS。', 0),
('cv2.Canny', 'OpenCV', 'cv2.Canny(src, threshold1, threshold2) 边缘检测，threshold1 为低阈值，threshold2 为高阈值，通常 threshold2≈2~3倍 threshold1。双阈值决定边缘连接；值越低检测到的边缘越多但噪声也多。', 0),
('cv2.threshold', 'OpenCV', 'cv2.threshold(src, thresh, maxval, type) 固定阈值分割，返回 (retval, dst)。type 常用 cv2.THRESH_BINARY(二值)、cv2.THRESH_OTSU(与 THRESH_BINARY 组合可自动求阈值，此时 thresh 传 0)。', 0),
('cv2.Sobel', 'OpenCV', 'cv2.Sobel(src, ddepth, dx, dy, ksize) 索贝尔梯度算子，dx/dy 表示求导方向，ddepth 常用 cv2.CV_64F 后取绝对值 cv2.convertScaleAbs 避免负值截断。', 0),
('cv2.Laplacian', 'OpenCV', 'cv2.Laplacian(src, ddepth) 拉普拉斯算子，对噪声敏感，常先高斯平滑；可检测图像边缘和灰度突变区域。', 0),
('cv2.getStructuringElement', 'OpenCV', 'cv2.getStructuringElement(shape, ksize) 生成形态学结构元素，shape 可选 cv2.MORPH_RECT(矩形)、cv2.MORPH_ELLIPSE(椭圆)、cv2.MORPH_CROSS(十字形)。结构元素形状和大小直接影响腐蚀膨胀效果。', 0),
('cv2.SIFT_create', 'OpenCV', 'sift = cv2.SIFT_create() 创建 SIFT 检测器；kp, des = sift.detectAndCompute(img, None) 提取关键点和描述子。注意较新 OpenCV 中 SIFT 在 cv2.SIFT_create 而非 xfeatures2d。', 0),
('cv2.createBackgroundSubtractorMOG2', 'OpenCV', 'bg = cv2.createBackgroundSubtractorMOG2() 创建 MOG2 背景建模器；fgmask = bg.apply(frame) 得到前景掩膜。可设置 history(历史帧数)、varThreshold(方差阈值)。', 0);

-- RAG 知识库：常见报错
INSERT INTO `rag_document` (`title`, `category`, `content`, `chunk_index`) VALUES
('BGR/RGB 颜色混淆', '报错库', '现象：用 matplotlib 显示 OpenCV 读取的图像颜色偏蓝/偏红。原因：OpenCV 用 BGR 顺序，matplotlib/PIL 用 RGB 顺序。解决：显示前用 cv2.cvtColor(img, cv2.COLOR_BGR2RGB) 转换。', 0),
('图像路径错误返回None', '报错库', '现象：AttributeError: NoneType object has no attribute shape。原因：cv2.imread 因路径错误/中文路径/文件不存在返回 None。解决：检查路径是否正确、是否存在中文、用绝对路径或 os.path.join，读取后判空。', 0),
('数值溢出', '报错库', '现象：图像运算后出现大片白色或黑色块。原因：np.uint8 类型加减运算溢出(超过255或小于0)。解决：使用 cv2.add/subtract 或先转 float32 运算再 clip(0,255) 后转回 uint8。', 0),
('核大小必须为奇数', '报错库', '现象：Assertion failed (ksize % 2 == 1)。原因：高斯/中值滤波等核大小必须是正奇数。解决：核大小设为奇数，如(3,3)、(5,5)。', 0),
('通道数不匹配', '报错库', '现象：shape 维度与期望不符。原因：灰度图(2维)与彩色图(3维)混用，或通道顺序错误。解决：检查 img.shape，必要时用 cv2.cvtColor 统一为灰度或彩色。', 0),
('SIFT 模块不存在', '报错库', '现象：AttributeError: module cv2 has no attribute xfeatures2d。原因：新版 OpenCV 已将 SIFT 移入主模块。解决：使用 cv2.SIFT_create()。', 0),
('阈值过高导致全黑', '报错库', '现象：二值化后整幅图为黑色。原因：threshold 阈值设置过高，所有像素都低于阈值。解决：用 Otsu 自动阈值，或先查看直方图选择合适阈值。', 0);
