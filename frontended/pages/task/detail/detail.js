const api = require('../../../utils/api');
const util = require('../../../utils/util');
const app = getApp();

Page({
  data: {
    task: null,
    publisher: null,
    loading: true,
    taking: false,
    _timer: null
  },

  onLoad(options) {
    const id = options.id;
    const userInfo = wx.getStorageSync('userInfo');
    this.setData({ userInfo: userInfo || {} });
    if (id) this.loadDetail(id);
    this.startPolling();
  },
  onShow() { if (this.data.task) { this.loadDetail(this.data.task.taskId); this.startPolling(); } },
  onHide(){ this.stopPolling(); },
  onUnload(){ this.stopPolling(); },
  startPolling(){ this.stopPolling(); var s=this; s.data._timer=setInterval(function(){ if(s.data.task) s.loadDetail(s.data.task.taskId, true); },3000); },
  stopPolling(){ if(this.data._timer){clearInterval(this.data._timer);this.data._timer=null;} },

  async loadDetail(id, fromPoll) {
    if (this.data._loading) return;
    if (!fromPoll) this.setData({ loading: true });
    this.setData({ _loading: true });
    try {
      const task = await api.get('/api/task/detail/' + id);
      this.setData({ task, loading: false, _loading: false });
      if (task.publisherId) {
        const publisher = await api.get('/api/user/simple/' + task.publisherId);
        this.setData({ publisher });
      }
    } catch (e) {
      this.setData({ loading: false, _loading: false });
    }
  },

  async takeTask() {
    if (!app.checkVerified()) return;
    const userInfo = wx.getStorageSync('userInfo');
    const res = await new Promise(r => {
      wx.showModal({ title: '确认接单', content: '接单后请在截止时间前完成送达', success: r });
    });
    if (!res.confirm) return;

    this.setData({ taking: true });
    try {
      await api.post('/api/task/take/' + this.data.task.taskId);
      wx.showToast({ title: '接单成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (e) {}
    this.setData({ taking: false });
  },

  async completeTask() {
    const task = this.data.task;
    const isPublisher = task.publisherId === this.data.userInfo.userId;
    const title = isPublisher ? '确认完成' : '确认送达';
    const content = isPublisher
      ? '确认跑腿任务已完成？\n接单者ID：' + (task.takerId || '未知')
      : '确认你已送达，任务完成？';
    const res = await new Promise(r => {
      wx.showModal({ title: title, content: content, success: r });
    });
    if (!res.confirm) return;
    try {
      var body = isPublisher ? { takerId: task.takerId } : {};
      await api.post('/api/task/complete/' + task.taskId, body);
      wx.showToast({ title: '已确认完成', icon: 'success' });
      var targetId = isPublisher ? task.takerId : task.publisherId;
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/evaluation/evaluation?orderId=' + task.taskId +
               '&orderType=0&targetId=' + targetId + '&isTask=1&otherId=' + (isPublisher ? task.publisherId : task.takerId)
        });
      }, 800);
    } catch (e) {
      wx.showToast({ title: '操作失败', icon: 'none' });
    }
  },

  async cancelTask() {
    const res = await new Promise(r => {
      wx.showModal({ title: '取消任务', content: '确认取消该跑腿任务？', success: r });
    });
    if (!res.confirm) return;
    try {
      await api.post('/api/task/cancel/' + this.data.task.taskId);
      wx.showToast({ title: '已取消', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (e) {}
  },

  async giveUpTask() {
    var res = await new Promise(function(r) {
      wx.showModal({ title: '放弃任务', content: '确定放弃这个任务？放弃后任务将重新变为待接单状态。', success: r });
    });
    if (!res.confirm) return;
    try {
      await api.post('/api/task/giveup/' + this.data.task.taskId);
      wx.showToast({ title: '已放弃', icon: 'success' });
      setTimeout(function() { wx.navigateBack(); }, 1000);
    } catch(e) { wx.showToast({ title: '操作失败', icon: 'none' }); }
  },

  editTask() {
    wx.navigateTo({
      url: '/pages/task/publish/publish?taskId=' + this.data.task.taskId
    });
  },

  evaluatePublisher() {
    wx.navigateTo({
      url: '/pages/evaluation/evaluation?orderId=' + this.data.task.taskId +
           '&orderType=0&targetId=' + this.data.task.publisherId + '&isTask=1'
    });
  },

  contactPublisher() {
    wx.navigateTo({ url: '/pages/chat/chat?otherId=' + this.data.task.publisherId });
  },

  contactTaker() {
    wx.navigateTo({ url: '/pages/chat/chat?otherId=' + this.data.task.takerId });
  },

  doReport() {
    var t = this.data.task;
    wx.setStorageSync('reportContext', { type: 'task', id: t.taskId, title: t.remark || '跑腿任务' });
    wx.navigateTo({ url: '/pages/report/publish/publish' });
  }
});
