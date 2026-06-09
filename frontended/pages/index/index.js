const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    userInfo: {},
    greeting: '',
    greetingHint: '',
    activeCircleTab: 0,
    circlePosts: [],
    loading: true
  },

  onLoad() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.setData({ userInfo: wx.getStorageSync('userInfo') || {} });
    this.initGreeting();
    this.loadData();
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    var userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({ userInfo: userInfo });
    this.initGreeting();
    this.loadData();
    this.loadBadges();
  },

  async loadBadges() {
    try {
      var counts = await api.get('/api/user/badge');
      if (counts) {
        var unread = counts.unreadMessages || 0;
        if (unread > 0) {
          wx.setTabBarBadge({ index: 3, text: String(unread > 99 ? '99+' : unread) });
        } else {
          wx.removeTabBarBadge({ index: 3 });
        }
      }
    } catch(e) {}
  },

  initGreeting() {
    var hour = new Date().getHours();
    var userInfo = wx.getStorageSync('userInfo') || {};
    var name = userInfo.nickName || '同学';
    if (hour < 9) {
      this.setData({ greeting: '早上好，' + name, greetingHint: '又是元气满满的一天' });
    } else if (hour < 12) {
      this.setData({ greeting: '上午好，' + name, greetingHint: '课间来逛逛校园圈吧' });
    } else if (hour < 14) {
      this.setData({ greeting: '中午好，' + name, greetingHint: '午休时间，聊聊最近的新鲜事' });
    } else if (hour < 18) {
      this.setData({ greeting: '下午好，' + name, greetingHint: '下课后有什么安排？' });
    } else if (hour < 22) {
      this.setData({ greeting: '晚上好，' + name, greetingHint: '今天过得怎么样？' });
    } else {
      this.setData({ greeting: '夜深了，' + name, greetingHint: '早点休息，明天见' });
    }
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      var posts = await api.get('/api/announcement/list', { type: this.data.activeCircleTab });
      this.setData({
        circlePosts: (posts || []).slice(0, 8),
        loading: false
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  switchCircleTab(e) {
    var type = parseInt(e.currentTarget.dataset.type);
    this.setData({ activeCircleTab: type });
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().then(function() { wx.stopPullDownRefresh(); });
  },

  goTo(e) {
    var url = e.currentTarget.dataset.url;
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    var tabPages = ['/pages/goods/list/list', '/pages/task/square/square', '/pages/index/index', '/pages/message/message', '/pages/user/user'];
    if (tabPages.indexOf(url) >= 0) {
      wx.switchTab({ url: url });
    } else {
      wx.navigateTo({ url: url });
    }
  },

  goToCampusCircle(e) {
    var id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/campus-circle/detail/detail?id=' + id });
  }
});
