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
        'Content-Type': 'application/json', // 表示请求体是 JSON 格式
        'Authorization': token || '' // JWT token 放请求头里，用于身份认证
      },

      success(res) {
        // 1. 如果 HTTP 状态码是 401，说明 token 无效或过期
        if (res.statusCode === 401) { // 未登录 → 跳登录页
          // 清除本地缓存和全局数据中的登录信息
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          // 强制跳转到登录页面（注意这里用 navigateTo，因为登录页一般不是 tabBar 页）
          wx.navigateTo({ url: '/pages/login/login' });
          reject(new Error('未登录'));
          return;
        }
        // 2. 判断后端返回的 JSON 中的业务状态码 code 是否为 200
        if (res.data.code === 200) {
          // 成功：把业务数据 res.data.data 传出去，这就是 await api.post() 拿到的结果
          resolve(res.data.data);
        } else {                     
          // 失败 → 弹提示
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          // 将错误通过 reject 抛出，上层 catch 可以捕获
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

/**
 * 上传文件函数（例如上传图片）
 * @param {string} filePath 本地临时文件路径
 */
function uploadFile(filePath) {
  const token = wx.getStorageSync('token');
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: baseUrl + '/api/oss/upload',
      filePath,
      name: 'file',
      header: { 'Authorization': token || '' },
      success(res) {
        // 上传接口返回的是字符串，需要手动解析成 JSON。
        // 注意：401 时后端返回空 body，直接 JSON.parse 会抛异常导致 Promise 永不结束
        let data;
        try {
          data = JSON.parse(res.data);
        } catch (e) {
          reject(new Error(res.statusCode === 401 ? '请先登录' : '上传失败(' + res.statusCode + ')'));
          return;
        }
        if (data.code === 200) resolve(data.data);
        else reject(new Error(data.message));
      },
      fail: reject
    });
  });
}
/**
 * 获取文件签名地址（用来访问私有文件或生成带签名的临时链接）
 */
function signUrl(key) {
  return request('/api/oss/sign-url?key=' + encodeURIComponent(key), 'GET');
}
// 构造 WebSocket 地址
// 把 baseUrl 中的 http 协议替换为 ws，https 替换为 wss，再拼接上聊天接口路径
var wsUrl = baseUrl.replace('http:', 'ws:').replace('https:', 'wss:') + '/ws/chat';

// 统一导出（暴露给外部使用的接口）
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
