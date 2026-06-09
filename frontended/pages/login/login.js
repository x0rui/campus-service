const api = require('../../utils/api');
const app = getApp();

Page({
  data: {},

  onLoad() {
    const token = wx.getStorageSync('token');
    if (token) {
      wx.checkSession({
        success: () => wx.switchTab({ url: '/pages/index/index' }),
        fail: () => wx.removeStorageSync('token')
      });
    }
  },

  handleLogin() {
    wx.showLoading({ title: '登录中...', mask: true });
    wx.login({
      success: (res) => {
        if (!res.code) {
          wx.hideLoading();
          wx.showToast({ title: '获取微信凭证失败', icon: 'none' });
          return;
        }
        this.doLogin(res.code);
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('wx.login失败:', err);
        wx.showToast({ title: '微信登录失败，请重试', icon: 'none' });
      }
    });
  },

  async doLogin(code) {
    try {
      const result = await api.post('/api/user/login', {
        code: code,
        nickName: '微信用户',
        avatarUrl: ''
      });

      wx.setStorageSync('token', result.token);
      wx.setStorageSync('userInfo', result.user);
      app.globalData.token = result.token;
      app.globalData.userInfo = result.user;

      wx.hideLoading();
      wx.switchTab({ url: '/pages/index/index' });
    } catch (e) {
      wx.hideLoading();
      console.error('登录请求失败:', e);
      wx.showToast({ title: '登录失败，请检查网络', icon: 'none' });
    }
  }
});
