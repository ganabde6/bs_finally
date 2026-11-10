<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">课程管理</h1>
        <p class="page-subtitle">管理学科体系与教师任课</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" :icon="Plus" @click="openAdd">新增学科</el-button>
      </div>
    </div>
    <!-- 学科管理 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学科管理</span>
        </div>
      </template>
      <el-table :data="subjects" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="subjectName" label="学科名称" width="160" />
        <el-table-column prop="subjectCode" label="学科编码" width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delSubject(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 教师任课 -->
    <el-card class="mt-20">
      <template #header><span>教师任课</span></template>
      <el-form :inline="true" class="mb-20">
        <el-form-item label="选择教师">
          <el-select v-model="teacherId" filterable @change="loadCourses">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName + '(' + t.username + ')'" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template v-if="teacherId">
        <el-table v-if="courses.length" :data="courses" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="班级"><template #default="{row}">{{ classText(row.classId) }}</template></el-table-column>
          <el-table-column label="学科"><template #default="{row}">{{ subjectText(row.subjectId) }}</template></el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{row}">
              <el-button size="small" @click="openEditCourse(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeCourseRow(row.id)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <template v-else>
          <div class="assign-tip" style="margin: 0 0 12px">该教师暂无任课。每位教师只任教一科:请指定其学科与班级</div>
          <el-form :inline="true">
            <el-form-item label="班级">
              <el-select v-model="newForm.classId" style="width:160px">
                <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="学科">
              <el-select v-model="newForm.subjectId" style="width:160px">
                <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNewCourse">确定任课</el-button>
            </el-form-item>
          </el-form>
        </template>
      </template>
      <el-empty v-else description="请选择教师查看任课情况" />
    </el-card>

    <!-- 新增/编辑学科 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑学科' : '新增学科'" width="440">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学科名称"><el-input v-model="form.subjectName" /></el-form-item>
        <el-form-item label="学科编码"><el-input v-model="form.subjectCode" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSubject">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任课 -->
    <el-dialog v-model="editVisible" title="编辑任课" width="440">
      <el-form label-width="100px">
        <el-form-item label="班级">
          <el-select v-model="editForm.classId">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" :disabled="isClassTaken(c.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="editForm.subjectId">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
          <div v-if="courses.length > 1" class="assign-tip">每位教师只任教一科:修改学科将同步该教师的全部任课</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEditCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  adminSubjects, addSubject, updateSubject, deleteSubject,
  adminClasses, teacherCourses, assignCourse, updateCourse, removeCourse,
  pageUsers, getRoles
} from '@/api'

const subjects = ref([])
const classes = ref([])
const teachers = ref([])
const courses = ref([])
const loading = ref(false)
const teacherId = ref(null)
const formVisible = ref(false)
const form = reactive({})
const editVisible = ref(false)
const editForm = reactive({ id: null, classId: null, subjectId: null })
const newForm = reactive({ classId: null, subjectId: null })

onMounted(async () => {
  const [s, c, r] = await Promise.all([adminSubjects(), adminClasses(), getRoles()])
  subjects.value = s.data
  classes.value = c.data
  const teacherRole = r.data.find(x => x.roleCode === 'TEACHER')
  if (teacherRole) {
    const tRes = await pageUsers({ current: 1, size: 200, roleId: teacherRole.id })
    teachers.value = tRes.data.records
  }
})

const classText = (id) => classes.value.find(c => c.id === id)?.className || '-'
const subjectText = (id) => subjects.value.find(s => s.id === id)?.subjectName || '-'

// 编辑弹窗:该教师其他任课已占用的班级(同学科)置灰
const isClassTaken = (classId) =>
  courses.value.some(r => r.id !== editForm.id && r.classId === classId && r.subjectId === editForm.subjectId)
const openEditCourse = (row) => {
  editForm.id = row.id
  editForm.classId = row.classId
  editForm.subjectId = row.subjectId
  editVisible.value = true
}

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.sort = 0
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const saveSubject = async () => {
  if (!form.subjectName || !form.subjectCode) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.id) await updateSubject(form)
  else await addSubject(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  const res = await adminSubjects()
  subjects.value = res.data
}

const delSubject = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该学科?', '提示', { type: 'warning' })
  } catch { return }
  await deleteSubject(id)
  ElMessage.success('已删除')
  const res = await adminSubjects()
  subjects.value = res.data
}

const loadCourses = async () => {
  if (!teacherId.value) return
  const res = await teacherCourses(teacherId.value)
  courses.value = res.data
}

const saveEditCourse = async () => {
  if (!editForm.classId || !editForm.subjectId) {
    ElMessage.warning('请选择班级和学科')
    return
  }
  await updateCourse(editForm.id, { classId: editForm.classId, subjectId: editForm.subjectId })
  ElMessage.success('已保存')
  editVisible.value = false
  loadCourses()
}

const saveNewCourse = async () => {
  if (!newForm.classId || !newForm.subjectId) {
    ElMessage.warning('请选择班级和学科')
    return
  }
  await assignCourse({ teacherId: teacherId.value, classId: newForm.classId, subjectId: newForm.subjectId })
  ElMessage.success('任课已指定')
  loadCourses()
}

const removeCourseRow = async (id) => {
  try {
    await ElMessageBox.confirm('确定移除该任课?', '提示', { type: 'warning' })
  } catch { return }
  await removeCourse(id)
  ElMessage.success('已移除')
  loadCourses()
}
</script>

<style scoped>
.assign-tip {
  width: 100%;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
