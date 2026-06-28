// 引入项目自定义的网络请求模块，里面封装了 get/post/put 等方法
const api = require('../../utils/api');
const app = getApp(); // 获取小程序全局唯一的应用实例对象，可以用来读写全局数据和方法

// 使用 Page() 注册一个页面
Page({
  data: {},
  onLoad() {
    // 从本地缓存中同步读取 token（之前登录成功后存进去的）
    const token = wx.getStorageSync('token');
    // 如果本地有 token，进一步检查这个 token 是否还有效
    if (token) {
       // 检查微信登录态是否过期（与微信服务器的会话状态）
      wx.checkSession({
        // 会话有效：说明之前登录过且未过期，直接跳转到首页
        success: () => wx.switchTab({ url: '/pages/index/index' }),
        // 会话失效：清除本地缓存的 token，让用户重新登录
        fail: () => wx.removeStorageSync('token')
      });
    }
    // 如果本地没有 token，页面就正常显示登录按钮，等待用户点击登录
  },

  // login.wxml绑定了bindtap="handleLogin"，用户点击登录按钮后触发这个函数
  handleLogin() {
    // 显示加载提示框，mask:true 表示用户不能点击穿透，防止重复操作
    wx.showLoading({ title: '登录中...', mask: true });

    // 调用微信登录接口，获取临时登录凭证 code
    //code 是前端通过调用 wx.login() 接口获取的，然后发送给后端。
    wx.login({
      success: (res) => {

        // 如果微信没有返回 code，提示失败
        if (!res.code) {
          wx.hideLoading();
          wx.showToast({ title: '获取微信凭证失败', icon: 'none' });
          return;
        }

        // 拿到 code 后，调用 doLogin 方法，把 code 发给自己的后端换取 token
        this.doLogin(res.code);
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('wx.login失败:', err);
        wx.showToast({ title: '微信登录失败，请重试', icon: 'none' });
      }
    });
  },

  // 用 async 声明的异步方法，负责将 code 发送到后端完成登录
  async doLogin(code) {
    try {
      // 调用 api.post 发送 POST 请求到 /api/user/login
      // 参数：code 是必须的，nickName 和 avatarUrl 暂时给了默认值
      const result = await api.post('/api/user/login', {
        code: code,
        nickName: '微信用户',
        avatarUrl: ''
      });

      // 登录成功后，后端会返回 token（身份凭证）和用户信息
      // 1. 把 token 和用户信息存入本地缓存（下次打开小程序可以直接读取）
      wx.setStorageSync('token', result.token);
      wx.setStorageSync('userInfo', result.user);
      // 2. 同时存到全局变量中，方便其他页面直接使用（无需再次读缓存）
      app.globalData.token = result.token;
      app.globalData.userInfo = result.user;

      // 隐藏加载框，然后跳转到首页
      wx.hideLoading();
      wx.switchTab({ url: '/pages/index/index' });
    } catch (e) {
      // 请求失败（网络错误、后端返回异常等）会进入这里
      wx.hideLoading();
      console.error('登录请求失败:', e);
      wx.showToast({ title: '登录失败，请检查网络', icon: 'none' });
    }
  }
});
