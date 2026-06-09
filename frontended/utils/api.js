const baseUrl = 'http://192.168.0.162:8081';

function request(url, method = 'GET', data = {}) {
  // 从本地缓存拿 token（之前登录时存的）
  const token = wx.getStorageSync('token');
  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + url, // 拼完整地址
      method, // GET/POST/PUT/DELETE
      data, // 请求参数
      header: {
        'Content-Type': 'application/json',
        'Authorization': token || '' // JWT token 放请求头里
      },
      success(res) {
        if (res.statusCode === 401) { // 未登录 → 跳登录页
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          wx.navigateTo({ url: '/pages/login/login' });
          reject(new Error('未登录'));
          return;
        }
        if (res.data.code === 200) { // 成功 → 返回 data
          resolve(res.data.data);
        } else {                     // 失败 → 弹提示
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(new Error(res.data.message));
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
}

function uploadFile(filePath) {
  const token = wx.getStorageSync('token');
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: baseUrl + '/api/oss/upload',
      filePath,
      name: 'file',
      header: { 'Authorization': token || '' },
      success(res) {
        const data = JSON.parse(res.data);
        if (data.code === 200) resolve(data.data);
        else reject(new Error(data.message));
      },
      fail: reject
    });
  });
}

function signUrl(key) {
  return request('/api/oss/sign-url?key=' + encodeURIComponent(key), 'GET');
}

var wsUrl = baseUrl.replace('http:', 'ws:').replace('https:', 'wss:') + '/ws/chat';

module.exports = {
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  put: (url, data) => request(url, 'PUT', data),
  del: (url, data) => request(url, 'DELETE', data),
  uploadFile,
  signUrl,
  baseUrl,
  wsUrl
};
