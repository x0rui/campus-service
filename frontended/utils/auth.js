function checkAuth() {
  const token = wx.getStorageSync('token');
  if (!token) {
    wx.navigateTo({ url: '/pages/login/login' });
    return false;
  }
  return true;
}

function checkRole(requiredRole) {
  const userInfo = wx.getStorageSync('userInfo');
  if (!userInfo) {
    wx.navigateTo({ url: '/pages/login/login' });
    return false;
  }
  if (userInfo.role < requiredRole) {
    wx.showToast({ title: '权限不足', icon: 'none' });
    return false;
  }
  return true;
}

module.exports = { checkAuth, checkRole };
