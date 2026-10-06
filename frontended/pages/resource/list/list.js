const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    list: [],
    page: 0,
    hasMore: true,
    loading: false,
    keyword: '',
    courses: ['全部课程'],
    courseIndex: 0,
    activeCourse: '',
    types: ['全部', '课件', '笔记', '真题', '代码', '其他'],
    activeType: ''
  },

  onLoad() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.loadCourses();
    this.loadList();
  },

  async loadCourses() {
    try {
      const cs = await api.get('/api/resource/courses');
      this.setData({ courses: ['全部课程'].concat(cs || []) });
    } catch (e) {}
  },

  async loadList() {
    if (this.data.loading || !this.data.hasMore) return;
    this.setData({ loading: true });
    try {
      const q = { page: this.data.page };
      if (this.data.activeCourse) q.course = this.data.activeCourse;
      if (this.data.activeType) q.type = this.data.activeType;
      if (this.data.keyword.trim()) q.keyword = this.data.keyword.trim();
      const arr = (await api.get('/api/resource/list', q)) || [];
      this.setData({
        list: this.data.page === 0 ? arr : this.data.list.concat(arr),
        page: this.data.page + 1,
        hasMore: arr.length >= 10
      });
    } catch (e) {}
    this.setData({ loading: false });
  },

  reset() {
    this.setData({ page: 0, hasMore: true, list: [] });
    this.loadList();
  },

  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },
  onSearch() { this.reset(); },

  onCourseChange(e) {
    const i = Number(e.detail.value);
    this.setData({ courseIndex: i, activeCourse: i === 0 ? '' : this.data.courses[i] });
    this.reset();
  },

  filterType(e) {
    const t = e.currentTarget.dataset.type;
    this.setData({ activeType: t === '全部' ? '' : t });
    this.reset();
  },

  onReachBottom() { this.loadList(); },

  onPullDownRefresh() {
    this.reset();
    wx.stopPullDownRefresh();
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/resource/detail/detail?id=' + e.currentTarget.dataset.id });
  },
  goPublish() {
    if (!app.checkLogin()) return;
    wx.navigateTo({ url: '/pages/resource/publish/publish' });
  },
  goDemand() {
    wx.navigateTo({ url: '/pages/resource/demand-list/demand-list' });
  },
  goMy() {
    wx.navigateTo({ url: '/pages/resource/my/my' });
  }
});
