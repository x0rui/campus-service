const api = require('../../../utils/api');
const app = getApp();

// 跑腿任务实时位置：
// 接单者 → 持续定位上报（存 Redis，后端同时经 WebSocket 推给发布者）
// 发布者 → 收 WebSocket 推送，收不到就 3 秒轮询兜底
Page({
  data: {
    taskId: null,
    isTaker: false,
    lat: null,
    lng: null,
    markers: [],
    updatedAt: '',
    uploading: false,
    statusText: ''
  },

  onLoad(options) {
    const u = wx.getStorageSync('userInfo') || {};
    const takerId = options.takerId ? Number(options.takerId) : null;
    const isTaker = takerId !== null && takerId === u.userId;
    this.setData({
      taskId: options.taskId,
      isTaker: isTaker,
      statusText: isTaker ? '正在上报你的位置' : '等待接单者上报位置'
    });
    if (isTaker) this.startUpload();
    else this.startListen();
  },

  onUnload() { this.cleanup(); },

  cleanup() {
    if (this.uploadTimer) clearInterval(this.uploadTimer);
    if (this.pollTimer) clearInterval(this.pollTimer);
    if (wx.stopLocationUpdate) { try { wx.stopLocationUpdate({}); } catch (e) {} }
    if (this.socketTask) { try { this.socketTask.close({}); } catch (e) {} }
  },

  applyLocation(lat, lng, time) {
    this.setData({
      lat: lat,
      lng: lng,
      updatedAt: time || '',
      statusText: '接单者位置已更新',
      markers: [{ id: 1, latitude: lat, longitude: lng, width: 32, height: 32 }]
    });
  },

  // ---- 接单者：上报位置 ----
  startUpload() {
    const self = this;
    const report = function (lat, lng) {
      self.applyLocation(lat, lng, '');
      self.setData({ statusText: '位置上报中' });
      api.post('/api/task/location/report', { taskId: self.data.taskId, lat: lat, lng: lng }).catch(function () {});
    };

    if (wx.startLocationUpdate) {
      wx.startLocationUpdate({
        success() {
          if (wx.onLocationChange) {
            wx.onLocationChange(function (res) { report(res.latitude, res.longitude); });
            self.setData({ uploading: true });
          } else {
            self.fallbackUpload(report);
          }
        },
        fail() { self.fallbackUpload(report); }
      });
    } else {
      this.fallbackUpload(report);
    }
  },

  // 降级：持续定位不可用时，按固定间隔上报
  fallbackUpload(report) {
    const self = this;
    const tick = function () {
      wx.getLocation({
        type: 'gcj02',
        success(res) { report(res.latitude, res.longitude); }
      });
    };
    tick();
    this.uploadTimer = setInterval(tick, 8000);
    this.setData({ uploading: true });
  },

  // ---- 发布者：WebSocket 为主，轮询兜底 ----
  startListen() {
    const self = this;
    const u = wx.getStorageSync('userInfo') || {};
    const token = wx.getStorageSync('token');
    let url = app.globalData.wsUrl + '/' + u.userId;
    if (token) url += '?token=' + encodeURIComponent(token);

    try {
      this.socketTask = wx.connectSocket({ url: url });
      this.socketTask.onMessage(function (res) {
        try {
          const d = JSON.parse(res.data);
          if (d && d.type === 'location') self.applyLocation(d.lat, d.lng, d.time);
        } catch (e) {}
      });
    } catch (e) {}

    this.pollTimer = setInterval(function () {
      api.get('/api/task/location/' + self.data.taskId).then(function (raw) {
        if (!raw) return;
        let d = raw;
        if (typeof raw === 'string') { try { d = JSON.parse(raw); } catch (e) { return; } }
        if (d && d.lat) self.applyLocation(d.lat, d.lng, d.time);
      }).catch(function () {});
    }, 3000);
  }
});
