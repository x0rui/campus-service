const api = require('../../../utils/api');
const util = require('../../../utils/util');
const app = getApp();

Page({
  data: {
    userInfo: null,
    taskList: [],
    activeType: '',
    taskTypes: ['全部', '取快递', '取外卖', '代买', '代办', '自定义'],
    keyword: '',
    loading: false
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.loadTasks();
  },

  async loadTasks() {
    this.setData({ loading: true });
    try {
      const tasks = await api.get('/api/task/square', { page: 0 });
      this.setData({ taskList: tasks || [], loading: false });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async filterByType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ activeType: type });
    if (type === '全部') {
      this.loadTasks();
      return;
    }
    this.setData({ loading: true });
    try {
      const tasks = await api.get('/api/task/type/' + type);
      this.setData({ taskList: tasks || [], loading: false });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  goToDetail(e) {
    wx.navigateTo({ url: '/pages/task/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  goToPublish() {
    if (!app.checkLogin()) return;
    wx.navigateTo({ url: '/pages/task/publish/publish' });
  },

  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },

  async onSearch() {
    if (!this.data.keyword.trim()) { this.loadTasks(); return; }
    this.setData({ loading: true });
    try {
      var tasks = await api.get('/api/task/search', { keyword: this.data.keyword });
      this.setData({ taskList: tasks || [], loading: false });
    } catch (e) { this.setData({ loading: false }); }
  },

  onPullDownRefresh() {
    this.loadTasks().then(function() {
      wx.stopPullDownRefresh();
    });
  }
});
