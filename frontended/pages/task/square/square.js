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
    loading: false,
    nearby: false,
    myLat: null,
    myLng: null
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.loadTasks();
  },

  // 附近优先开关：拿一次定位，之后列表按到取件点的距离升序
  toggleNearby() {
    const self = this;
    if (this.data.nearby) {
      this.setData({ nearby: false, myLat: null, myLng: null });
      this.loadTasks();
      return;
    }
    wx.getLocation({
      type: 'gcj02',
      success(res) {
        self.setData({ nearby: true, myLat: res.latitude, myLng: res.longitude });
        self.loadTasks();
      },
      fail() { wx.showToast({ title: '需要位置权限才能按距离排序', icon: 'none' }); }
    });
  },

  // 把米格式化成好读的距离
  fmt(list) {
    return (list || []).map(function (t) {
      const m = t.distanceMeters;
      if (m === null || m === undefined) { t.distanceText = ''; }
      else if (m < 1000) { t.distanceText = Math.round(m) + 'm'; }
      else { t.distanceText = (m / 1000).toFixed(1) + 'km'; }
      return t;
    });
  },

  async loadTasks() {
    this.setData({ loading: true });
    try {
      let tasks;
      if (this.data.nearby && this.data.myLat !== null) {
        tasks = await api.get('/api/task/nearby', {
          lat: this.data.myLat, lng: this.data.myLng,
          type: this.data.activeType === '全部' ? '' : this.data.activeType,
          page: 0
        });
      } else {
        tasks = await api.get('/api/task/square', { page: 0 });
      }
      this.setData({ taskList: this.fmt(tasks), loading: false });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async filterByType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ activeType: type });
    if (this.data.nearby) { this.loadTasks(); return; }
    if (type === '全部') {
      this.loadTasks();
      return;
    }
    this.setData({ loading: true });
    try {
      const tasks = await api.get('/api/task/type/' + type);
      this.setData({ taskList: this.fmt(tasks), loading: false });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  goToDetail(e) {
    wx.navigateTo({ url: '/pages/task/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  // 任务进行中时看接单者实时位置
  goTracking(e) {
    wx.navigateTo({
      url: '/pages/task/tracking/tracking?taskId=' + e.currentTarget.dataset.id +
           '&takerId=' + (e.currentTarget.dataset.taker || '')
    });
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
