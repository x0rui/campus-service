const api = require('./utils/api');

App({
  globalData: {
    token: '',
    userInfo: null,
    baseUrl: api.baseUrl,
    wsUrl: api.wsUrl
  },

  onLaunch() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
    }
  },

  // 每次切回前台时刷新 badge（让红点更快显示）
  onShow() {
    var token = wx.getStorageSync('token');
    if (!token) return;
    api.get('/api/user/badge').then(function(counts) {
      if (!counts) return;
      var unread = counts.unreadMessages || 0;
      if (unread > 0) {
        wx.setTabBarBadge({ index: 3, text: String(unread > 99 ? '99+' : unread) });
      } else {
        wx.removeTabBarBadge({ index: 3 });
      }
    }).catch(function(){});
  },

  checkLogin() {
    if (!this.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return false;
    }
    return true;
  },

  checkVerified() {
    if (!this.checkLogin()) return false;
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.realName) {
      wx.showModal({
        title: '尚未实名认证',
        content: '请先完成实名认证后再进行操作',
        confirmText: '去认证',
        success: function(res) {
          if (res.confirm) {
            wx.switchTab({ url: '/pages/user/user' });
          }
        }
      });
      return false;
    }
    return true;
  }
});
