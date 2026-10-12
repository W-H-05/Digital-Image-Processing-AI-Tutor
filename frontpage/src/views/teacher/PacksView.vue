<template>
  <div>
    <div class="header">
      <h2>📦 课次包管理</h2>
      <el-button type="primary" @click="openCreate">➕ 新建课次包</el-button>
    </div>

    <div class="pack-list">
      <div v-for="pack in packs" :key="pack.id" class="pack-item card">
        <div class="pack-main">
          <div class="pack-head">
            <span class="pack-no">{{ pack.lessonNo }}</span>
            <span class="pack-title">{{ pack.title }}</span>
            <el-tag :type="statusType(pack.status)" size="small">{{ statusLabel(pack.status) }}</el-tag>
          </div>
          <div class="pack-chapter">章节：{{ pack.chapter || '—' }}</div>
        </div>
        <div class="pack-actions">
          <el-button size="small" @click="openEdit(pack)">编辑</el-button>
          <el-button size="small" @click="openMaterials(pack)">材料管理</el-button>
          <el-button size="small" @click="openCoursewares(pack)">课件管理</el-button>
          <el-button size="small" @click="openHomework(pack)">作业管理</el-button>
          <el-button size="small" @click="openPractice(pack)">练习管理</el-button>
          <el-button size="small" type="success" v-if="pack.status !== 'PUBLISHED'" @click="publish(pack)">发布</el-button>
          <el-button size="small" type="warning" v-if="pack.status === 'PUBLISHED'" @click="offline(pack)">下架</el-button>
          <el-button size="small" @click="copy(pack)">复制</el-button>
          <el-button size="small" type="danger" link @click="remove(pack)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑课次包' : '新建课次包'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课次号"><el-input v-model="form.lessonNo" placeholder="如：第1次课 / 实验1" /></el-form-item>
        <el-form-item label="学时"><el-input v-model="form.hours" placeholder="如：2学时" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="章节"><el-input v-model="form.chapter" /></el-form-item>
        <el-form-item label="内容概述"><el-input v-model="form.contentSummary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="教学目标"><el-input v-model="form.objectives" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="重点"><el-input v-model="form.keyPoints" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="难点"><el-input v-model="form.difficultPoints" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="思政元素"><el-input v-model="form.ideologicalNotes" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 材料管理抽屉 -->
    <el-drawer v-model="materialDrawer" :title="'材料管理：' + currentPack?.title" size="650px">
      <div class="section-title">📤 上传文件材料</div>
      <div class="upload-area">
        <el-form inline>
          <el-form-item label="标题">
            <el-input v-model="uploadForm.title" placeholder="材料标题" style="width: 160px" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="uploadForm.materialType" placeholder="类型" style="width: 120px">
              <el-option v-for="t in uploadTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="分组">
            <el-select v-model="uploadForm.groupName" placeholder="分组" style="width: 120px">
              <el-option label="课前预习" value="课前预习" />
              <el-option label="课中讲解" value="课中讲解" />
              <el-option label="课后作业" value="课后作业" />
              <el-option label="拓展阅读" value="拓展阅读" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="upload-row">
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            :on-change="onFileSelect"
            accept=".pdf,.ppt,.pptx,.doc,.docx,.md,.png,.jpg,.jpeg,.gif,.webp,.bmp,.html,.htm,.zip,.mp4"
          >
            <el-button type="primary">📁 选择文件</el-button>
          </el-upload>
          <span v-if="uploadForm.file" class="selected-file">已选：{{ uploadForm.file.name }}（{{ formatSize(uploadForm.file.size) }}）</span>
          <el-button type="success" :disabled="!uploadForm.file" :loading="uploading" @click="doUpload">上传</el-button>
        </div>
        <div class="upload-tip">支持 PDF / PPT / DOC / Markdown / 图片 / HTML / ZIP / 视频链接，上传后可在下方逐项控制是否开放</div>
      </div>

      <el-divider />
      <div class="section-title">📋 文本材料快速添加</div>
      <div class="mat-add">
        <el-input v-model="newMat.title" placeholder="材料标题" style="width: 160px" />
        <el-select v-model="newMat.materialType" placeholder="类型" style="width: 120px">
          <el-option v-for="t in matTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="newMat.groupName" placeholder="分组" style="width: 120px">
          <el-option label="课前预习" value="课前预习" />
          <el-option label="课中讲解" value="课中讲解" />
          <el-option label="课后作业" value="课后作业" />
          <el-option label="拓展阅读" value="拓展阅读" />
        </el-select>
        <el-button type="primary" @click="addTextMat">添加</el-button>
      </div>

      <el-divider />
      <div class="section-title">🗂️ 已上传材料（{{ materials.length }}）<span class="sort-tip">↕ 拖动调整顺序</span></div>
      <div v-for="m in materials" :key="m.id" class="mat-row" draggable="true"
           @dragstart="onDragStart(m)" @dragover.prevent @drop="onDrop(m)">
        <span class="drag-handle">⠿</span>
        <span class="mat-icon">{{ iconFor(m.materialType) }}</span>
        <div class="mat-info">
          <div class="mat-title">{{ m.title }}</div>
          <div class="mat-desc">
            {{ m.materialType }} · {{ m.groupName || '未分组' }} · v{{ m.version || 1 }}
            <span v-if="m.filePath" class="file-link">📎 {{ m.fileType || '文件' }}</span>
          </div>
        </div>
        <div class="mat-controls">
          <el-switch v-model="m.isOpenToStudent" :active-value="1" :inactive-value="0" active-text="开放" @change="updateMat(m)" />
          <el-button size="small" link @click="showVersions(m)">版本</el-button>
          <el-button size="small" type="danger" link @click="removeMat(m)">删除</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 版本历史抽屉 -->
    <el-drawer v-model="versionDrawer" :title="`版本历史：${versionMaterial?.title || ''}`" size="500px">
      <div v-if="versions.length === 0" class="empty">暂无历史版本</div>
      <div v-for="v in versions" :key="v.id" class="version-item">
        <div class="version-head">
          <span class="version-no">v{{ v.version }}</span>
          <span class="version-title">{{ v.title }}</span>
        </div>
        <div class="version-meta">{{ v.fileType || '文本' }} · {{ formatTime(v.createTime) }}</div>
        <el-button size="small" type="primary" link @click="restoreVersion(v)">恢复此版本</el-button>
      </div>
    </el-drawer>

    <!-- 课件管理抽屉 -->
    <el-drawer v-model="coursewareDrawer" :title="'课件管理：' + currentPack?.title" size="650px">
      <div class="section-title">🎛️ 新建交互课件</div>
      <div class="cw-form card">
        <el-form label-width="90px">
          <el-form-item label="课件名称">
            <el-input v-model="cwForm.name" placeholder="如：灰度直方图实验室" />
          </el-form-item>
          <el-form-item label="课件类型">
            <el-select v-model="cwForm.type" placeholder="选择课件类型">
              <el-option v-for="t in cwTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="参数配置">
            <el-input v-model="cwForm.configJson" type="textarea" :rows="4" placeholder='JSON 格式，如 {"min":0,"max":255,"checkpoint":"观察直方图形态"}' />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveCourseware">{{ editingCw ? '保存修改' : '创建课件' }}</el-button>
            <el-button v-if="editingCw" @click="cancelCwEdit">取消编辑</el-button>
          </el-form-item>
        </el-form>
        <div class="tip">💡 课件类型会决定前端渲染的交互控件（滑块/按钮），参数 JSON 定义范围与任务检查点</div>
      </div>

      <el-divider />
      <div class="section-title">🗂️ 已有课件（{{ coursewares.length }}）</div>
      <div v-for="cw in coursewares" :key="cw.id" class="cw-row">
        <div class="cw-info">
          <div class="cw-name">🎛️ {{ cw.name }}</div>
          <div class="cw-type">{{ cw.type }}<span v-if="cw.resourcePath" class="cw-has-img"> · 有示例图</span></div>
        </div>
        <div class="cw-actions">
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="(f) => onSampleImage(cw, f)">
            <el-button size="small">🖼 上传示例图</el-button>
          </el-upload>
          <el-button size="small" @click="editCourseware(cw)">编辑</el-button>
          <el-button size="small" type="danger" link @click="removeCourseware(cw)">删除</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 作业管理抽屉 -->
    <el-drawer v-model="homeworkDrawer" :title="'作业管理：' + currentPack?.title" size="700px">
      <div class="section-title">📤 上传作业文件（txt/md，自动识别题目）</div>
      <div class="hw-upload">
        <el-input v-model="hwTitle" placeholder="作业标题" style="width: 220px" />
        <el-upload :show-file-list="false" :auto-upload="false" accept=".txt,.md" :on-change="onHomeworkFile">
          <el-button type="primary">📁 选择作业文件</el-button>
        </el-upload>
        <span v-if="hwFileName" class="selected-file">已选：{{ hwFileName }}</span>
      </div>
      <div class="upload-tip">格式说明：每道题以「数字. [单选/填空/代码] 题目」开头，选择题选项为 A. B. C. D.，答案行「答案：X」</div>

      <!-- 解析预览 -->
      <div v-if="parsedQuestions.length" class="hw-preview">
        <div class="section-title">📋 解析预览（{{ parsedQuestions.length }} 题，共 {{ parsedTotal }} 分）</div>
        <div v-for="(q, i) in parsedQuestions" :key="i" class="q-preview">
          <div class="q-stem"><span class="q-tag">{{ typeLabel(q.type) }}</span> {{ q.stem }}</div>
          <div v-if="q.options" class="q-options">
            <div v-for="(o, j) in q.options" :key="j" class="q-option">{{ o }}</div>
          </div>
          <div class="q-answer">答案：{{ q.answer || '（略）' }} · {{ q.score }} 分</div>
        </div>
        <div class="hw-save-row">
          <el-button type="primary" @click="saveHomework">💾 保存作业</el-button>
        </div>
      </div>

      <el-divider />
      <div class="section-title">🗂️ 已有作业（{{ homeworks.length }}）</div>
      <div v-for="h in homeworks" :key="h.id" class="hw-item">
        <div class="hw-info">
          <div class="hw-name">📝 {{ h.title }}</div>
          <div class="hw-meta">共 {{ h.totalScore }} 分</div>
        </div>
        <div class="hw-actions">
          <el-button size="small" @click="viewSubmissions(h)">查看提交</el-button>
          <el-button size="small" type="primary" @click="viewAnalysis(h)">题目分析</el-button>
          <el-button size="small" type="danger" link @click="removeHomework(h)">删除</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 作业提交查看 -->
    <el-drawer v-model="submissionDrawer" :title="'作业提交：' + currentHomework?.title" size="560px">
      <div v-if="submissions.length === 0" class="empty">暂无学生提交</div>
      <div v-for="s in submissions" :key="s.id" class="sub-item">
        <div class="sub-row">
          <span class="sub-name">{{ s.studentName }}（{{ s.username }}）</span>
          <span class="sub-score">得分：<b>{{ s.score }}</b> 分</span>
          <span class="sub-time">{{ formatTime(s.submitTime) }}</span>
          <el-button size="small" type="danger" link @click="rejectSubmission(s)">打回重做</el-button>
        </div>
        <div v-if="s.answers && Object.keys(s.answers).length" class="sub-answers">
          <div v-for="(val, qi) in s.answers" :key="qi" class="sub-ans-item">
            <span class="sub-ans-q">第 {{ Number(qi) + 1 }} 题：</span>
            <span class="sub-ans-v">{{ val || '（未作答）' }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 作业题目分析 -->
    <el-drawer v-model="analysisDrawer" :title="'题目分析：' + analysisData?.title" size="640px">
      <div v-if="!analysisData" class="empty">加载中…</div>
      <template v-else>
        <div class="ana-meta">共 {{ analysisData.submitCount }} 人提交 · 满分 {{ analysisData.totalScore }}</div>
        <div v-for="q in analysisData.questions" :key="q.index" class="ana-q">
          <div class="ana-q-head">
            <span class="ana-q-no">第 {{ q.index + 1 }} 题</span>
            <el-tag size="small" :type="q.correctRate >= 60 ? 'success' : 'danger'">正确率 {{ q.correctRate }}%</el-tag>
            <span class="ana-q-stem">{{ q.stem }}</span>
          </div>
          <!-- 选择题：各选项学生分布 -->
          <div v-if="q.type === 'choice'" class="ana-opts">
            <div v-for="o in q.options" :key="o.key" class="ana-opt" :class="{ right: o.isCorrect }">
              <span class="ana-opt-key">{{ o.key }}</span>
              <span class="ana-opt-text">{{ o.text }}</span>
              <span class="ana-opt-count">{{ o.count }} 人</span>
              <span class="ana-opt-tag" v-if="o.isCorrect">✓ 正确答案</span>
              <div class="ana-opt-students" v-if="o.students.length">{{ o.students.join('、') }}</div>
            </div>
          </div>
          <!-- 填空/代码题：正确与错误答案 -->
          <div v-else class="ana-opts">
            <div class="ana-group">
              <div class="ana-group-title">✅ 答对（{{ q.correctStudents?.length || 0 }} 人）</div>
              <div v-for="(c, i) in q.correctStudents" :key="'c' + i" class="ana-ans">
                <span class="ana-ans-stu">{{ c.student }}</span>
                <span class="ana-ans-val">{{ c.answer }}</span>
              </div>
              <div v-if="!q.correctStudents?.length" class="ana-none">暂无</div>
            </div>
            <div class="ana-group">
              <div class="ana-group-title wrong">❌ 答错（{{ q.wrongAnswers?.length || 0 }} 人）</div>
              <div v-for="(w, i) in q.wrongAnswers" :key="'w' + i" class="ana-ans">
                <span class="ana-ans-stu">{{ w.student }}</span>
                <span class="ana-ans-val wrong">{{ w.answer }}</span>
              </div>
              <div v-if="!q.wrongAnswers?.length" class="ana-none">暂无</div>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 练习管理抽屉 -->
    <el-drawer v-model="practiceDrawer" :title="'练习管理：' + currentPack?.title" size="700px">
      <!-- 新增/编辑题目表单 -->
      <div class="section-title">{{ editingPractice ? '✏️ 编辑题目' : '➕ 新增题目（自动题号）' }}</div>
      <div class="practice-form">
        <el-form label-width="80px">
          <el-form-item label="题干">
            <el-input v-model="practiceForm.stem" type="textarea" :rows="3" placeholder="输入题目内容" />
          </el-form-item>
          <el-form-item label="题干图片">
            <div class="stem-image-upload">
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                accept="image/*"
                :on-change="(f) => onStemImage(f)"
              >
                <el-button size="small">上传题干图片</el-button>
              </el-upload>
              <img v-if="practiceForm.stemImage" :src="practiceForm.stemImage" class="stem-image-preview" />
              <el-button v-if="practiceForm.stemImage" size="small" type="danger" link @click="practiceForm.stemImage = ''">移除</el-button>
            </div>
          </el-form-item>
          <el-form-item label="题型">
            <el-radio-group v-model="practiceForm.isMultiple">
              <el-radio :value="false">单选题</el-radio>
              <el-radio :value="true">多选题</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="选项">
            <div class="option-editor">
              <div v-for="(opt, i) in practiceForm.options" :key="i" class="option-row">
                <span class="option-key">{{ opt.key }}</span>
                <div class="option-main">
                  <el-input v-model="opt.text" :placeholder="'选项 ' + opt.key + ' 内容'" />
                  <div class="option-image-row">
                    <el-upload
                      :show-file-list="false"
                      :auto-upload="false"
                      accept="image/*"
                      :on-change="(f) => onOptionImage(opt, f)"
                    >
                      <el-button size="small">上传选项图片</el-button>
                    </el-upload>
                    <img v-if="opt.image" :src="opt.image" class="option-image-preview" />
                    <el-button v-if="opt.image" size="small" type="danger" link @click="opt.image = ''">移除</el-button>
                  </div>
                </div>
                <el-checkbox
                  :model-value="practiceForm.correctKeys.includes(opt.key)"
                  @change="(v) => toggleCorrectKey(opt.key, v)"
                  :disabled="practiceForm.options.length < 2"
                >正确</el-checkbox>
                <el-button size="small" type="danger" link @click="removeOption(i)" :disabled="practiceForm.options.length <= 2">删除</el-button>
              </div>
              <el-button size="small" @click="addOption" :disabled="practiceForm.options.length >= 8">➕ 添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="savePracticeQuestion">{{ editingPractice ? '保存修改' : '添加题目' }}</el-button>
            <el-button v-if="editingPractice" @click="cancelPracticeEdit">取消编辑</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />
      <div class="section-title">🗂️ 已有题目（{{ practiceQuestions.length }}）</div>
      <div v-if="practiceQuestions.length === 0" class="empty">暂无题目，请添加</div>
      <div v-for="q in practiceQuestions" :key="q.id" class="practice-q-item">
        <div class="practice-q-info">
          <div class="practice-q-head">
            <span class="practice-q-no">第 {{ q.questionNo }} 题</span>
            <el-tag v-if="q.isMultiple === 1" type="warning" size="small">多选</el-tag>
            <el-tag v-else type="info" size="small">单选</el-tag>
          </div>
          <div class="practice-q-stem">{{ q.stem }}</div>
          <img v-if="q.stemImage" :src="q.stemImage" class="practice-q-stem-img" />
          <div class="practice-q-opts">
            <span v-for="o in q.options" :key="o.key" class="pq-opt" :class="{ correct: (q.correctKeys || []).includes(o.key) }">
              {{ o.key }}. {{ o.text }}<img v-if="o.image" :src="o.image" class="pq-opt-img" />
            </span>
          </div>
        </div>
        <div class="practice-q-actions">
          <el-button size="small" @click="editPracticeQuestion(q)">编辑</el-button>
          <el-button size="small" type="danger" link @click="removePracticeQuestion(q)">删除</el-button>
        </div>
      </div>

      <el-divider />
      <div class="section-title">📊 练习学情</div>
      <div v-if="practiceStatsData" class="practice-stats">
        <div class="ps-row">
          <span>题目数：<b>{{ practiceStatsData.questionCount }}</b></span>
          <span>总作答：<b>{{ (practiceStatsData.totalCorrect || 0) + (practiceStatsData.totalWrong || 0) }}</b> 次</span>
          <span>正确率：<b :class="{ good: practiceStatsData.totalAccuracy >= 60, bad: practiceStatsData.totalAccuracy < 60 }">{{ practiceStatsData.totalAccuracy }}%</b></span>
        </div>
        <div v-if="practiceStatsData.perQuestion?.length" class="ps-perq">
          <div v-for="q in practiceStatsData.perQuestion" :key="q.questionId" class="ps-q">
            <span class="ps-q-no">第{{ q.questionNo }}题</span>
            <div class="ps-bar"><div class="ps-fill" :class="{ good: q.accuracy >= 60, bad: q.accuracy < 60 }" :style="{ width: q.accuracy + '%' }"></div></div>
            <span class="ps-q-acc">{{ q.accuracy }}%</span>
          </div>
        </div>
        <div v-if="practiceStatsData.wrongReasons?.length" class="ps-reasons">
          <div class="section-title" style="margin-top:8px">💬 错因汇总</div>
          <div v-for="(r, i) in practiceStatsData.wrongReasons" :key="i" class="ps-reason">
            <span class="ps-reason-count">{{ r.count }}人</span>
            <span class="ps-reason-text">{{ r.reason }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listLessonPacks, createLessonPack, updateLessonPack, deleteLessonPack,
  publishLessonPack, offlineLessonPack, copyLessonPack,
  listMaterials, addTextMaterial, updateMaterial, deleteMaterial, uploadMaterial,
  sortMaterials, getMaterialVersions, restoreMaterialVersion,
  listCoursewares, createCourseware, updateCourseware, deleteCourseware, uploadCoursewareSample,
  parseHomeworkFile, saveHomework as apiSaveHomework, listHomeworkForTeacher, deleteHomework, listHomeworkSubmissions, rejectHomeworkSubmission, homeworkAnalysis,
  listPracticeForTeacher, addPracticeQuestion, updatePracticeQuestion, deletePracticeQuestion, practiceStats, uploadPracticeImage
} from '../../api'

const packs = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({})
const materialDrawer = ref(false)
const coursewareDrawer = ref(false)
const versionDrawer = ref(false)
const currentPack = ref(null)
const materials = ref([])
const coursewares = ref([])
const versions = ref([])
const versionMaterial = ref(null)
const dragItem = ref(null)
const homeworkDrawer = ref(false)
const submissionDrawer = ref(false)
const analysisDrawer = ref(false)
const homeworks = ref([])
const analysisData = ref(null)
const hwTitle = ref('')
const hwFileName = ref('')
const hwFileRaw = ref(null)
// 练习管理
const practiceDrawer = ref(false)
const practiceQuestions = ref([])
const practiceStatsData = ref(null)
const editingPractice = ref(false)
const editingPracticeId = ref(null)
const practiceForm = ref({ stem: '', isMultiple: false, options: [], correctKeys: [] })
const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const parsedQuestions = ref([])
const parsedTotal = ref(0)
const currentHomework = ref(null)
const submissions = ref([])
const newMat = ref({ title: '', materialType: '', groupName: '' })
const matTypes = ['重难点', '知识点', '作业', '其他', '图片素材']
const uploadTypes = ['PPT', '教案', '知识点', '图片素材', '交互课件', '作业', '其他']
const cwTypes = ['直方图', '均衡化', '低通', '频域', '形态学', '分割', '边缘', '视频']

// 上传表单
const uploadForm = ref({ title: '', materialType: '', groupName: '', file: null })
const uploading = ref(false)

// 课件表单
const cwForm = ref({ name: '', type: '', configJson: '' })
const editingCw = ref(false)

onMounted(load)

async function load() {
  packs.value = await listLessonPacks()
}

function statusType(s) { return s === 'PUBLISHED' ? 'success' : s === 'OFFLINE' ? 'info' : 'warning' }
function statusLabel(s) { return s === 'PUBLISHED' ? '已发布' : s === 'OFFLINE' ? '已下架' : '草稿' }
function iconFor(type) {
  const map = { '教案': '📋', '重难点': '🎯', '知识点': '📘', 'PPT': '📊', '交互课件': '🎛️', '图片素材': '🖼️', '作业': '📝', '其他': '📎', '备课笔记': '🔒' }
  return map[type] || '📄'
}
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function openCreate() {
  editing.value = false
  form.value = { courseId: 1 }
  dialogVisible.value = true
}

function openEdit(pack) {
  editing.value = true
  form.value = { ...pack }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return }
  if (editing.value) await updateLessonPack(form.value.id, form.value)
  else await createLessonPack(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function publish(pack) { await publishLessonPack(pack.id); ElMessage.success('已发布'); load() }
async function offline(pack) { await offlineLessonPack(pack.id); ElMessage.success('已下架'); load() }
async function copy(pack) { await copyLessonPack(pack.id); ElMessage.success('已复制'); load() }

async function remove(pack) {
  try {
    await ElMessageBox.confirm(`确定删除课次包「${pack.title}」？`, '删除', { type: 'warning' })
    await deleteLessonPack(pack.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {}
}

// ===== 材料管理 =====
async function openMaterials(pack) {
  currentPack.value = pack
  materialDrawer.value = true
  await refreshMaterials()
}

async function refreshMaterials() {
  materials.value = await listMaterials(currentPack.value.id)
}

function onFileSelect(file) {
  uploadForm.value.file = file.raw
}

async function doUpload() {
  if (!uploadForm.value.file) { ElMessage.warning('请先选择文件'); return }
  if (!uploadForm.value.title) uploadForm.value.title = uploadForm.value.file.name
  if (!uploadForm.value.materialType) uploadForm.value.materialType = '其他'
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadForm.value.file)
    fd.append('title', uploadForm.value.title)
    fd.append('materialType', uploadForm.value.materialType)
    fd.append('groupName', uploadForm.value.groupName || '')
    fd.append('isOpenToStudent', '1')
    await uploadMaterial(currentPack.value.id, fd)
    ElMessage.success('上传成功！')
    uploadForm.value = { title: '', materialType: '', groupName: '', file: null }
    await refreshMaterials()
  } catch (e) {
  } finally {
    uploading.value = false
  }
}

async function addTextMat() {
  if (!newMat.value.title) { ElMessage.warning('请输入标题'); return }
  await addTextMaterial(currentPack.value.id, { ...newMat.value, description: '', isOpenToStudent: 1, allowDownload: 1 })
  ElMessage.success('已添加')
  newMat.value = { title: '', materialType: '', groupName: '' }
  await refreshMaterials()
}

async function updateMat(m) {
  await updateMaterial(m.id, {
    materialType: m.materialType, title: m.title, description: m.description,
    groupName: m.groupName, isOpenToStudent: m.isOpenToStudent,
    allowDownload: m.allowDownload, sortIndex: m.sortIndex
  })
  ElMessage.success('已更新')
}

async function removeMat(m) {
  try {
    await ElMessageBox.confirm(`确定删除材料「${m.title}」？`, '删除', { type: 'warning' })
    await deleteMaterial(m.id)
    ElMessage.success('已删除')
    await refreshMaterials()
  } catch (e) {}
}

// ===== 拖拽排序 =====
function onDragStart(m) {
  dragItem.value = m
}

async function onDrop(target) {
  if (!dragItem.value || dragItem.value.id === target.id) return
  const list = [...materials.value]
  const fromIdx = list.findIndex(x => x.id === dragItem.value.id)
  const toIdx = list.findIndex(x => x.id === target.id)
  if (fromIdx < 0 || toIdx < 0) return
  list.splice(toIdx, 0, list.splice(fromIdx, 1)[0])
  materials.value = list
  // 提交新顺序
  const items = list.map((m, i) => ({ id: m.id, sortIndex: i }))
  try {
    await sortMaterials(items)
    ElMessage.success('排序已更新')
  } catch (e) {}
  dragItem.value = null
}

// ===== 版本管理 =====
async function showVersions(m) {
  versionMaterial.value = m
  versionDrawer.value = true
  versions.value = await getMaterialVersions(m.id)
}

async function restoreVersion(v) {
  try {
    await ElMessageBox.confirm(`确定恢复到 v${v.version} 版本？当前内容会先保存为历史版本`, '恢复版本', { type: 'warning' })
    await restoreMaterialVersion(versionMaterial.value.id, v.id)
    ElMessage.success('已恢复')
    await refreshMaterials()
    versions.value = await getMaterialVersions(versionMaterial.value.id)
  } catch (e) {}
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

// ===== 作业管理 =====
async function openHomework(pack) {
  currentPack.value = pack
  homeworkDrawer.value = true
  hwTitle.value = ''
  hwFileName.value = ''
  hwFileRaw.value = null
  parsedQuestions.value = []
  parsedTotal.value = 0
  homeworks.value = await listHomeworkForTeacher(pack.id)
}

function onHomeworkFile(file) {
  hwFileRaw.value = file.raw
  hwFileName.value = file.raw.name
  if (!hwTitle.value) hwTitle.value = file.raw.name.replace(/\.(txt|md)$/i, '')
  parseHomework()
}

async function parseHomework() {
  if (!hwFileRaw.value) return
  const fd = new FormData()
  fd.append('file', hwFileRaw.value)
  try {
    const res = await parseHomeworkFile(fd)
    parsedQuestions.value = res.questions
    parsedTotal.value = res.totalScore
    ElMessage.success(`解析成功，共 ${res.count} 题`)
  } catch (e) {}
}

async function saveHomework() {
  if (!hwTitle.value) { ElMessage.warning('请输入作业标题'); return }
  if (parsedQuestions.value.length === 0) { ElMessage.warning('请先解析题目'); return }
  await apiSaveHomework({
    lessonPackId: currentPack.value.id,
    title: hwTitle.value,
    questionsJson: JSON.stringify(parsedQuestions.value)
  })
  ElMessage.success('作业已保存')
  homeworks.value = await listHomeworkForTeacher(currentPack.value.id)
}

async function removeHomework(h) {
  try {
    await ElMessageBox.confirm(`确定删除作业「${h.title}」？`, '删除', { type: 'warning' })
    await deleteHomework(h.id)
    ElMessage.success('已删除')
    homeworks.value = await listHomeworkForTeacher(currentPack.value.id)
  } catch (e) {}
}

async function viewSubmissions(h) {
  currentHomework.value = h
  submissionDrawer.value = true
  submissions.value = await listHomeworkSubmissions(h.id)
}

async function viewAnalysis(h) {
  currentHomework.value = h
  analysisDrawer.value = true
  analysisData.value = null
  analysisData.value = await homeworkAnalysis(h.id)
}

async function rejectSubmission(s) {
  try {
    await ElMessageBox.confirm(`确定打回 ${s.studentName}（${s.username}）的作业，允许其重新提交？`, '打回重做', { type: 'warning' })
    await rejectHomeworkSubmission(s.id)
    ElMessage.success('已打回，学生可重新提交')
    submissions.value = await listHomeworkSubmissions(currentHomework.value.id)
  } catch (e) {}
}

// ==================== 练习管理 ====================
async function openPractice(pack) {
  currentPack.value = pack
  practiceDrawer.value = true
  resetPracticeForm()
  await loadPractice()
}

function resetPracticeForm() {
  editingPractice.value = false
  editingPracticeId.value = null
  practiceForm.value = { stem: '', stemImage: '', isMultiple: false, options: [], correctKeys: [] }
  // 默认两个选项 A、B
  addOption()
  addOption()
}

function addOption() {
  const next = LETTERS[practiceForm.value.options.length]
  if (!next) return
  practiceForm.value.options.push({ key: next, text: '', image: '' })
}

function removeOption(i) {
  const removed = practiceForm.value.options.splice(i, 1)[0]
  // 移除对应的正确项
  const ci = practiceForm.value.correctKeys.indexOf(removed.key)
  if (ci >= 0) practiceForm.value.correctKeys.splice(ci, 1)
}

function toggleCorrectKey(key, checked) {
  const keys = practiceForm.value.correctKeys
  const idx = keys.indexOf(key)
  if (practiceForm.value.isMultiple) {
    // 多选：可勾选多个
    if (checked && idx < 0) keys.push(key)
    else if (!checked && idx >= 0) keys.splice(idx, 1)
  } else {
    // 单选：只能一个正确项
    keys.splice(0, keys.length)
    if (checked) keys.push(key)
  }
}

/** 上传题干图片 */
async function onStemImage(file) {
  const raw = file.raw
  if (!raw || !raw.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  const fd = new FormData()
  fd.append('file', raw)
  try {
    const res = await uploadPracticeImage(fd)
    practiceForm.value.stemImage = res.url
    ElMessage.success('题干图片已上传')
  } catch (e) {}
}

/** 上传选项图片 */
async function onOptionImage(opt, file) {
  const raw = file.raw
  if (!raw || !raw.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  const fd = new FormData()
  fd.append('file', raw)
  try {
    const res = await uploadPracticeImage(fd)
    opt.image = res.url
    ElMessage.success('选项图片已上传')
  } catch (e) {}
}

async function loadPractice() {
  practiceQuestions.value = await listPracticeForTeacher(currentPack.value.id)
  practiceStatsData.value = await practiceStats(currentPack.value.id).catch(() => null)
}

async function savePracticeQuestion() {
  const stem = practiceForm.value.stem.trim()
  const stemImage = practiceForm.value.stemImage || ''
  if (!stem && !stemImage) { ElMessage.warning('请输入题干或上传题干图片'); return }
  const options = practiceForm.value.options.filter(o => o.text.trim() || o.image)
  if (options.length < 2) { ElMessage.warning('至少需要 2 个有内容的选项'); return }
  if (practiceForm.value.correctKeys.length === 0) { ElMessage.warning('请设置正确答案'); return }
  const payload = {
    stem,
    stemImage,
    options: options.map(o => ({ key: o.key, text: (o.text || '').trim(), image: o.image || '' })),
    correctKeys: practiceForm.value.correctKeys,
    isMultiple: practiceForm.value.isMultiple
  }
  if (editingPractice.value) {
    await updatePracticeQuestion(editingPracticeId.value, payload)
    ElMessage.success('已更新')
  } else {
    await addPracticeQuestion(currentPack.value.id, payload)
    ElMessage.success('已添加')
  }
  resetPracticeForm()
  addOption()
  addOption()
  await loadPractice()
}

function editPracticeQuestion(q) {
  editingPractice.value = true
  editingPracticeId.value = q.id
  practiceForm.value = {
    stem: q.stem,
    stemImage: q.stemImage || '',
    isMultiple: q.isMultiple === 1,
    options: (q.options || []).map(o => ({ key: o.key, text: o.text || '', image: o.image || '' })),
    correctKeys: [...(q.correctKeys || [])]
  }
}

function cancelPracticeEdit() {
  resetPracticeForm()
  addOption()
  addOption()
}

async function removePracticeQuestion(q) {
  try {
    await ElMessageBox.confirm(`确定删除第 ${q.questionNo} 题？删除后题号自动重排`, '删除', { type: 'warning' })
    await deletePracticeQuestion(q.id)
    ElMessage.success('已删除')
    await loadPractice()
  } catch (e) {}
}

function typeLabel(t) {
  const map = { choice: '单选题', blank: '填空题', code: '代码题' }
  return map[t] || t
}

// ===== 课件管理 =====
async function openCoursewares(pack) {
  currentPack.value = pack
  coursewareDrawer.value = true
  editingCw.value = false
  cwForm.value = { name: '', type: '', configJson: '' }
  await refreshCoursewares()
}

async function refreshCoursewares() {
  coursewares.value = await listCoursewares(currentPack.value.id)
}

async function saveCourseware() {
  if (!cwForm.value.name) { ElMessage.warning('请输入课件名称'); return }
  if (!cwForm.value.type) { ElMessage.warning('请选择课件类型'); return }
  // 校验 configJson
  let config = cwForm.value.configJson
  if (config) {
    try { JSON.parse(config) } catch (e) { ElMessage.warning('参数配置必须是合法 JSON'); return }
  }
  if (editingCw.value) {
    await updateCourseware(cwForm.value.id, cwForm.value)
    ElMessage.success('课件已更新')
  } else {
    await createCourseware(currentPack.value.id, cwForm.value)
    ElMessage.success('课件已创建')
  }
  editingCw.value = false
  cwForm.value = { name: '', type: '', configJson: '' }
  await refreshCoursewares()
}

function editCourseware(cw) {
  editingCw.value = true
  cwForm.value = { id: cw.id, name: cw.name, type: cw.type, configJson: cw.configJson || '' }
}

async function onSampleImage(cw, file) {
  const raw = file.raw
  if (!raw || !raw.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  const fd = new FormData()
  fd.append('file', raw)
  try {
    await uploadCoursewareSample(cw.id, fd)
    ElMessage.success('示例图已上传')
    await refreshCoursewares()
  } catch (e) {}
}

function cancelCwEdit() {
  editingCw.value = false
  cwForm.value = { name: '', type: '', configJson: '' }
}

async function removeCourseware(cw) {
  try {
    await ElMessageBox.confirm(`确定删除课件「${cw.name}」？`, '删除', { type: 'warning' })
    await deleteCourseware(cw.id)
    ElMessage.success('已删除')
    await refreshCoursewares()
  } catch (e) {}
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.pack-list { display: flex; flex-direction: column; gap: 12px; }
.pack-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; flex-wrap: wrap; gap: 10px; }
.pack-head { display: flex; align-items: center; gap: 10px; }
.pack-no { color: #4F46E5; font-weight: 600; font-size: 13px; }
.pack-title { font-weight: 600; font-size: 16px; }
.pack-chapter { color: #94A3B8; font-size: 13px; margin-top: 4px; }
.pack-actions { display: flex; gap: 6px; flex-wrap: wrap; }

.section-title { font-weight: 700; margin-bottom: 12px; }
.upload-area { background: #FAFBFF; padding: 16px; border-radius: 12px; margin-bottom: 8px; }
.upload-row { display: flex; align-items: center; gap: 12px; margin-top: 4px; flex-wrap: wrap; }
.selected-file { color: #4F46E5; font-size: 13px; }
.upload-tip { font-size: 12px; color: #94A3B8; margin-top: 8px; }

.mat-add { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.mat-row { display: flex; align-items: center; gap: 10px; padding: 12px 0; border-bottom: 1px solid #F1F5F9; cursor: grab; }
.mat-row:active { cursor: grabbing; }
.drag-handle { color: #CBD5E1; font-size: 16px; cursor: grab; }
.sort-tip { font-size: 12px; color: #94A3B8; font-weight: 400; margin-left: 8px; }
.mat-icon { font-size: 12px; background: #EEF2FF; color: #4F46E5; padding: 4px 8px; border-radius: 8px; white-space: nowrap; }
.mat-info { flex: 1; }
.mat-title { font-weight: 500; font-size: 14px; }
.mat-desc { font-size: 12px; color: #94A3B8; }
.file-link { color: #06B6D4; }
.mat-controls { display: flex; align-items: center; gap: 8px; }

.version-item { padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.version-head { display: flex; align-items: center; gap: 8px; }
.version-no { background: #EEF2FF; color: #4F46E5; padding: 2px 8px; border-radius: 8px; font-size: 12px; font-weight: 600; }
.version-title { font-weight: 500; }
.version-meta { color: #94A3B8; font-size: 12px; margin: 6px 0; }

.empty { color: #94A3B8; text-align: center; padding: 30px; }

.cw-form { background: #FAFBFF; }
.cw-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.cw-name { font-weight: 500; font-size: 14px; }
.cw-type { font-size: 12px; color: #94A3B8; }
.cw-has-img { color: #16A34A; }
.cw-actions { display: flex; gap: 6px; }
.tip { font-size: 12px; color: #94A3B8; margin-top: 4px; }

.hw-upload { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 8px; }
.hw-preview { background: #FAFBFF; border-radius: 12px; padding: 14px; margin-top: 8px; }
.q-preview { padding: 10px 0; border-bottom: 1px solid #F1F5F9; }
.q-stem { font-weight: 500; color: #1F2937; }
.q-tag { background: #EEF2FF; color: #4F46E5; padding: 2px 8px; border-radius: 8px; font-size: 12px; margin-right: 6px; }
.q-options { padding-left: 16px; margin-top: 4px; }
.q-option { color: #475569; font-size: 13px; }
.q-answer { color: #16A34A; font-size: 12px; margin-top: 4px; }
.hw-save-row { margin-top: 12px; text-align: right; }
.hw-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.hw-name { font-weight: 500; }
.hw-meta { color: #94A3B8; font-size: 12px; }
.hw-actions { display: flex; gap: 6px; }
.sub-item { display: flex; flex-direction: column; gap: 8px; padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.sub-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.sub-name { font-weight: 500; }
.sub-score { color: #4F46E5; }
.sub-time { margin-left: auto; color: #94A3B8; font-size: 12px; }
.sub-answers { background: #FAFBFF; border-radius: 8px; padding: 8px 12px; }
.sub-ans-item { font-size: 12px; color: #475569; padding: 2px 0; }
.sub-ans-q { color: #94A3B8; }
.sub-ans-v { color: #1F2937; font-weight: 500; }

.ana-meta { color: #64748B; font-size: 13px; margin-bottom: 12px; }
.ana-q { padding: 14px 0; border-bottom: 1px solid #F1F5F9; }
.ana-q-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.ana-q-no { font-weight: 700; color: #4F46E5; }
.ana-q-stem { font-weight: 500; color: #1F2937; flex: 1; min-width: 200px; }
.ana-opts { display: flex; flex-direction: column; gap: 8px; }
.ana-opt { padding: 8px 12px; background: #FAFBFF; border-radius: 8px; border-left: 3px solid #E2E8F0; }
.ana-opt.right { border-left-color: #10B981; background: #F0FDF4; }
.ana-opt-key { font-weight: 700; margin-right: 6px; color: #4F46E5; }
.ana-opt-text { color: #374151; }
.ana-opt-count { color: #64748B; font-size: 12px; margin-left: 8px; }
.ana-opt-tag { color: #10B981; font-size: 12px; margin-left: 8px; font-weight: 600; }
.ana-opt-students { font-size: 12px; color: #64748B; margin-top: 4px; }
.ana-group { margin-top: 6px; }
.ana-group-title { font-weight: 600; font-size: 13px; margin-bottom: 4px; }
.ana-group-title.wrong { color: #DC2626; }
.ana-ans { display: flex; gap: 12px; padding: 4px 0; font-size: 13px; }
.ana-ans-stu { color: #475569; white-space: nowrap; }
.ana-ans-val { color: #1F2937; }
.ana-ans-val.wrong { color: #DC2626; }
.ana-none { color: #94A3B8; font-size: 12px; }

/* 练习管理 */
.option-editor { width: 100%; }
.option-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.option-key {
  width: 28px; height: 28px; border-radius: 50%;
  background: #EEF2FF; color: #4F46E5;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; flex-shrink: 0;
}
.option-main { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.option-image-row { display: flex; align-items: center; gap: 8px; }
.stem-image-upload { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.stem-image-preview { max-width: 200px; max-height: 120px; border-radius: 8px; border: 1px solid #E2E8F0; }
.option-image-preview { max-width: 90px; max-height: 60px; border-radius: 6px; border: 1px solid #E2E8F0; }
.practice-q-stem-img { max-width: 200px; max-height: 120px; border-radius: 8px; border: 1px solid #E2E8F0; margin: 6px 0; display: block; }
.pq-opt-img { max-width: 60px; max-height: 40px; border-radius: 4px; margin-left: 4px; vertical-align: middle; }
.practice-form { background: #FAFBFF; padding: 16px; border-radius: 10px; }
.practice-q-item { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.practice-q-info { flex: 1; }
.practice-q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.practice-q-no { font-weight: 700; color: #4F46E5; }
.practice-q-stem { color: #1F2937; font-size: 14px; margin-bottom: 6px; }
.practice-q-opts { display: flex; flex-wrap: wrap; gap: 6px; }
.pq-opt { font-size: 12px; background: #F1F5F9; color: #475569; padding: 2px 8px; border-radius: 8px; }
.pq-opt.correct { background: #ECFDF5; color: #10B981; border: 1px solid #10B981; }
.practice-q-actions { display: flex; gap: 4px; flex-shrink: 0; }
.practice-stats { background: #FAFBFF; padding: 14px; border-radius: 10px; }
.ps-row { display: flex; gap: 20px; margin-bottom: 12px; font-size: 14px; color: #475569; }
.ps-row b { font-size: 16px; }
.ps-row b.good { color: #10B981; }
.ps-row b.bad { color: #EF4444; }
.ps-perq { display: flex; flex-direction: column; gap: 8px; }
.ps-q { display: flex; align-items: center; gap: 10px; }
.ps-q-no { font-size: 12px; color: #64748B; white-space: nowrap; }
.ps-bar { flex: 1; height: 8px; background: #E2E8F0; border-radius: 4px; overflow: hidden; }
.ps-fill { height: 100%; border-radius: 4px; transition: width 0.4s; }
.ps-fill.good { background: #10B981; }
.ps-fill.bad { background: #EF4444; }
.ps-q-acc { font-size: 12px; color: #475569; min-width: 44px; text-align: right; }
.ps-reasons { margin-top: 10px; }
.ps-reason { display: flex; gap: 8px; padding: 6px 0; border-bottom: 1px dashed #E2E8F0; font-size: 13px; }
.ps-reason-count { color: #EF4444; font-weight: 600; flex-shrink: 0; }
.ps-reason-text { color: #475569; }
</style>
