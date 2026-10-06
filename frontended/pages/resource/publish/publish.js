const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    title: '',
    course: '',
    types: ['课件', '笔记', '真题', '代码', '其他'],
    typeIndex: 0,
    description: '',
    fileName: '',
    fileUrl: '',
    fileExt: '',
    fileSize: 0,
    uploading: false,
    submitting: false
  },

  onTitleInput(e) { this.setData({ title: e.detail.value }); },
  onCourseInput(e) { this.setData({ course: e.detail.value }); },
  onTypeChange(e) { this.setData({ typeIndex: Number(e.detail.value) }); },
  onDescInput(e) { this.setData({ description: e.detail.value }); },

  // 从聊天记录选文件（学生传资料主要靠这个）
  chooseFile() {
    const self = this;
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      success(res) {
        const f = res.tempFiles[0];
        self.upload(f.path, f.name, f.size);
      }
    });
  },

  // 也可以直接选相册里的图片当资料
  chooseImage() {
    const self = this;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success(res) {
        const f = res.tempFiles[0];
        const name = f.tempFilePath.split('/').pop();
        self.upload(f.tempFilePath, name, f.size);
      }
    });
  },

  async upload(path, name, size) {
    this.setData({ uploading: true });
    try {
      const url = await api.uploadFile(path);
      const ext = (name.split('.').pop() || '').toLowerCase();
      this.setData({ fileUrl: url, fileName: name, fileExt: ext, fileSize: size || 0 });
      wx.showToast({ title: '上传成功', icon: 'none' });
    } catch (e) {
      wx.showToast({ title: '上传失败', icon: 'none' });
    }
    this.setData({ uploading: false });
  },

  async submit() {
    const d = this.data;
    if (!d.title.trim()) return wx.showToast({ title: '请填写标题', icon: 'none' });
    if (!d.course.trim()) return wx.showToast({ title: '请填写课程', icon: 'none' });
    if (!d.fileUrl) return wx.showToast({ title: '请先选择并上传文件', icon: 'none' });
    if (d.submitting) return;

    this.setData({ submitting: true });
    try {
      await api.post('/api/resource/publish', {
        title: d.title.trim(),
        course: d.course.trim(),
        resourceType: d.types[d.typeIndex],
        description: d.description,
        fileName: d.fileName,
        fileUrl: d.fileUrl,
        fileExt: d.fileExt,
        fileSize: d.fileSize
      });
      wx.showToast({ title: '已提交，等待审核', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1200);
    } catch (e) {}
    this.setData({ submitting: false });
  }
});
