const api = require('../../../utils/api');

Page({
  data: {
    title: '',
    course: '',
    types: ['不限', '课件', '笔记', '真题', '代码', '其他'],
    typeIndex: 0,
    keyword: '',
    description: '',
    submitting: false
  },

  onTitleInput(e) { this.setData({ title: e.detail.value }); },
  onCourseInput(e) { this.setData({ course: e.detail.value }); },
  onTypeChange(e) { this.setData({ typeIndex: Number(e.detail.value) }); },
  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },
  onDescInput(e) { this.setData({ description: e.detail.value }); },

  async submit() {
    const d = this.data;
    if (!d.title.trim()) return wx.showToast({ title: '请填写需求标题', icon: 'none' });
    if (d.submitting) return;

    this.setData({ submitting: true });
    try {
      const res = await api.post('/api/resource-demand/publish', {
        title: d.title.trim(),
        course: d.course.trim(),
        resourceType: d.typeIndex === 0 ? '' : d.types[d.typeIndex],
        keyword: d.keyword.trim(),
        description: d.description
      });
      wx.showToast({ title: '已发布，匹配到' + (res.matchCount || 0) + '份', icon: 'none' });
      setTimeout(() => wx.redirectTo({ url: '/pages/resource/demand-match/demand-match?id=' + res.demandId }), 1200);
    } catch (e) {}
    this.setData({ submitting: false });
  }
});
