const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    dashboard: null, userList: [], goodsList: [], taskList: [], teamList: [], postList: [],
    reportList: [], clubApps: [], logList: [],
    currentTab: 0, loading: false,
    goodsKeyword: '', taskKeyword: '', teamKeyword: '', postKeyword: '', reportKeyword: '',
    userKeyword: '',
    postTab: 0, reportStatus: 0, reportType: 'all',
    badgePendingReports: 0, badgePendingClubChecks: 0,
    logKeyword: '', logType: 'all'
  },

  onLoad() {
    var userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || userInfo.role !== 2) {
      wx.showToast({ title: '仅管理员可访问', icon: 'none' });
      setTimeout(function() { wx.navigateBack(); }, 1500);
      return;
    }
    this.loadData();
    this.loadBadges();
  },

  async loadBadges() {
    try {
      var counts = await api.get('/api/user/badge');
      if (counts) {
        this.setData({
          badgePendingReports: counts.pendingReports || 0,
          badgePendingClubChecks: counts.pendingClubChecks || 0
        });
      }
    } catch(e) {}
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      var dashboard = await api.get('/api/admin/dashboard');
      this.setData({ dashboard: dashboard, loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },

  switchTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ currentTab: tab });
    this.loadBadges();
    if (tab === 0) this.loadData();
    else if (tab === 1) this.loadUsers();
    else if (tab === 2) this.loadAllGoods();
    else if (tab === 3) this.loadAllTasks();
    else if (tab === 4) this.loadAllTeams();
    else if (tab === 5) this.loadAllPosts();
    else if (tab === 6) this.loadReports();
    else if (tab === 7) this.loadClubApps();
    else if (tab === 8) this.loadLogs();
  },

  // ====== 用户管理 ======
  async loadUsers() {
    var kw = this.data.userKeyword.trim();
    try { var list = await api.get('/api/admin/users', kw ? { userId: kw } : {}); this.setData({ userList: list || [] }); } catch(e) {}
  },
  onUserKeyword(e) { this.setData({ userKeyword: e.detail.value }); },
  async searchUsers() {
    var kw = this.data.userKeyword.trim();
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/admin/users', kw ? { userId: kw } : {});
      this.setData({ userList: list || [], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  async setRole(e) {
    var id = e.currentTarget.dataset.id, role = e.currentTarget.dataset.role;
    var label = role === 1 ? '设为社团管理员' : '取消社团管理员';
    var self = this;
    wx.showModal({
      title: '确认操作', content: '确认' + label + '？',
      success: async function(res) {
        if (res.confirm) {
          try { await api.put('/api/admin/user-role/' + id, { role: role }); wx.showToast({ title: '操作成功', icon: 'success' }); self.loadUsers(); } catch(e) {}
        }
      }
    });
  },
  async toggleUserStatus(e) {
    var id = e.currentTarget.dataset.id, status = e.currentTarget.dataset.status;
    var label = status === 1 ? '禁用' : '启用';
    var self = this;
    wx.showModal({
      title: '确认操作', content: '确认' + label + '该用户？',
      success: async function(res) {
        if (res.confirm) {
          try { await api.put('/api/admin/user-status/' + id, { status: status }); wx.showToast({ title: '操作成功', icon: 'success' }); self.loadUsers(); } catch(e) {}
        }
      }
    });
  },
  showUserDetail(e) {
    var u = this.data.userList[e.currentTarget.dataset.index];
    wx.showModal({
      title: u.nickName || '用户',
      content: '用户ID: ' + u.userId + '\n角色: ' + (u.role===0?'学生':u.role===1?'社团管理员':'管理员') + '\n\n姓名: ' + (u.realName||'未认证') + '\n学号: ' + (u.studentId||'-') + '\n学院: ' + (u.college||'-') + '\n专业: ' + (u.major||'-') + '\n手机: ' + (u.phone||'-') + '\n\n状态: ' + (u.status===0?'正常':'已禁用'),
      showCancel: false
    });
  },

  // ====== 物品管理 ======
  async loadAllGoods() {
    this.setData({ loading: true });
    try { var list = await api.get('/api/admin/goods'); this.setData({ goodsList: list || [], loading: false }); } catch(e) { this.setData({ loading: false }); }
  },
  onGoodsKeyword(e) { this.setData({ goodsKeyword: e.detail.value }); },
  async searchGoods() {
    var kw = this.data.goodsKeyword.trim().toLowerCase();
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/admin/goods');
      if (kw) list = (list || []).filter(function(item) { return (item.title||'').toLowerCase().indexOf(kw) >= 0 || (item.category||'').toLowerCase().indexOf(kw) >= 0; });
      this.setData({ goodsList: list || [], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  async offlineGoods(e) {
    var id = e.currentTarget.dataset.id, self = this;
    wx.showModal({
      title: '确认下架', content: '确认下架该物品？',
      success: async function(res) {
        if (res.confirm) { try { await api.put('/api/admin/goods/offline/' + id, {}); wx.showToast({ title: '已下架', icon: 'success' }); self.loadAllGoods(); } catch(e) {} }
      }
    });
  },
  showGoodsDetail(e) {
    var g = this.data.goodsList[e.currentTarget.dataset.index];
    wx.showModal({ title: g.title, content: '价格: ¥' + g.price + '\n分类: ' + g.category + '\n\n发布者ID: ' + g.userId + '\n状态: ' + (g.status===0?'在售':g.status===1?'已售出':'已下架') + '\n\n发布时间: ' + (g.createTime||'-'), showCancel: false });
  },

  // ====== 任务管理 ======
  async loadAllTasks() {
    this.setData({ loading: true });
    try { var list = await api.get('/api/admin/tasks'); this.setData({ taskList: list || [], loading: false }); } catch(e) { this.setData({ loading: false }); }
  },
  onTaskKeyword(e) { this.setData({ taskKeyword: e.detail.value }); },
  async searchTasks() {
    var kw = this.data.taskKeyword.trim().toLowerCase();
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/admin/tasks');
      if (kw) list = (list || []).filter(function(item) { return (item.pickupLocation||'').toLowerCase().indexOf(kw) >= 0 || (item.deliveryLocation||'').toLowerCase().indexOf(kw) >= 0 || (item.remark||'').toLowerCase().indexOf(kw) >= 0; });
      this.setData({ taskList: list || [], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  async offlineTask(e) {
    var id = e.currentTarget.dataset.id, self = this;
    wx.showModal({
      title: '确认取消', content: '确认强制取消该任务？',
      success: async function(res) {
        if (res.confirm) { try { await api.put('/api/admin/task/offline/' + id, {}); wx.showToast({ title: '已取消', icon: 'success' }); self.loadAllTasks(); } catch(e) {} }
      }
    });
  },
  showTaskDetail(e) {
    var t = this.data.taskList[e.currentTarget.dataset.index];
    wx.showModal({ title: '跑腿任务 #' + t.taskId, content: '取件点: ' + t.pickupLocation + '\n送达点: ' + t.deliveryLocation + '\n\n跑腿费: ¥' + t.fee + '\n类型: ' + (t.taskType||'其他') + '\n状态: ' + (t.status===0?'待接单':t.status===1?'已接单':t.status===2?'已完成':'已取消') + '\n\n发布者: ' + t.publisherId + (t.takerId?'\n接单者: ' + t.takerId:''), showCancel: false });
  },

  // ====== 组局管理 ======
  async loadAllTeams() {
    this.setData({ loading: true });
    try { var list = await api.get('/api/team/list', { page: 0 }); this.setData({ teamList: list || [], loading: false }); } catch(e) { this.setData({ loading: false }); }
  },
  onTeamKeyword(e) { this.setData({ teamKeyword: e.detail.value }); },
  async searchTeams() {
    var kw = this.data.teamKeyword.trim().toLowerCase();
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/team/list', { page: 0 });
      if (kw) list = (list || []).filter(function(item) { return (item.title||'').toLowerCase().indexOf(kw) >= 0 || (item.description||'').toLowerCase().indexOf(kw) >= 0 || (item.tag||'').toLowerCase().indexOf(kw) >= 0; });
      this.setData({ teamList: list || [], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  async offlineTeam(e) {
    var id = e.currentTarget.dataset.id, self = this;
    wx.showModal({
      title: '确认结束', content: '确认强制结束该组局？',
      success: async function(res) {
        if (res.confirm) { try { await api.post('/api/team/cancel/' + id); wx.showToast({ title: '已结束', icon: 'success' }); self.loadAllTeams(); } catch(e) {} }
      }
    });
  },

  // ====== 校园圈管理 ======
  async loadAllPosts() {
    this.setData({ loading: true });
    try {
      var type = this.data.postTab;
      var list = type === 0 ? await api.get('/api/announcement/all') : await api.get('/api/announcement/list', { type: type - 1 });
      this.setData({ postList: list || [], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  onPostKeyword(e) { this.setData({ postKeyword: e.detail.value }); },
  async searchPosts() {
    var kw = this.data.postKeyword.trim().toLowerCase();
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/announcement/all');
      if (kw) list = (list || []).filter(function(item) { return (item.title||'').toLowerCase().indexOf(kw) >= 0 || (item.content||'').toLowerCase().indexOf(kw) >= 0; });
      this.setData({ postList: list || [], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  switchPostTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ postTab: tab });
    this.loadAllPosts();
  },
  async deletePost(e) {
    var id = e.currentTarget.dataset.id, self = this;
    wx.showModal({
      title: '确认删除', content: '确认删除该帖子？',
      success: async function(res) {
        if (res.confirm) { try { await api.del('/api/announcement/delete/' + id); wx.showToast({ title: '已删除', icon: 'success' }); self.loadAllPosts(); } catch(e) {} }
      }
    });
  },

  // ====== 举报管理 ======
  async loadReports() {
    this.setData({ loading: true });
    try {
      var status = this.data.reportStatus;
      var type = this.data.reportType;
      var list = await api.get('/api/report/admin/list', { targetType: type, status: status });
      list = (list || []).map(function(r) { r._note = ''; return r; });
      this.setData({ reportList: list, loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  onReportKeyword(e) { this.setData({ reportKeyword: e.detail.value }); },
  async searchReports() {
    var kw = this.data.reportKeyword.trim().toLowerCase();
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/report/admin/list', { targetType: this.data.reportType, status: this.data.reportStatus, keyword: kw });
      list = (list || []).map(function(r) { r._note = ''; return r; });
      this.setData({ reportList: list, loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },
  switchReportStatus(e) {
    var status = parseInt(e.currentTarget.dataset.status);
    this.setData({ reportStatus: status });
    this.loadReports();
  },
  switchReportType(e) {
    var type = e.currentTarget.dataset.type;
    this.setData({ reportType: type });
    this.loadReports();
  },
  onReportNote(e) {
    var id = e.currentTarget.dataset.id;
    var val = e.detail.value;
    var list = this.data.reportList;
    for (var i = 0; i < list.length; i++) { if (list[i].reportId == id) { list[i]._note = val; break; } }
    this.setData({ reportList: list });
  },
  async handleReport(e) {
    var id = e.currentTarget.dataset.id;
    var status = parseInt(e.currentTarget.dataset.status);
    var list = this.data.reportList;
    var note = '';
    for (var i = 0; i < list.length; i++) { if (list[i].reportId == id) { note = list[i]._note || ''; break; } }
    var label = status === 1 ? '确认违规' : '驳回';
    var self = this;
    wx.showModal({
      title: '确认操作', content: '确认' + label + '该举报？' + (note ? '\n备注: ' + note : ''),
      success: async function(res) {
        if (res.confirm) {
          try {
            await api.post('/api/report/admin/handle/' + id, { status: status, handleNote: note });
            wx.showToast({ title: '已' + label, icon: 'success' });
            self.loadReports();
          } catch(e) {}
        }
      }
    });
  },

  // ====== 社团审核 ======
  async loadClubApps() {
    try { var list = await api.get('/api/club/pending'); this.setData({ clubApps: list || [] }); } catch(e) {}
  },
  async approveClub(e) {
    var id = e.currentTarget.dataset.id, approved = e.currentTarget.dataset.approved === 'true';
    var self = this;
    wx.showModal({
      title: '确认', content: approved ? '通过该社团入驻申请？' : '拒绝该申请？',
      success: async function(res) {
        if (res.confirm) {
          try {
            var result = await api.put('/api/club/approve/' + id, { approved: approved });
            wx.showToast({ title: result || (approved ? '已通过' : '已拒绝'), icon: 'success' });
            self.loadClubApps();
          } catch(e) {}
        }
      }
    });
  },

  // ====== 操作日志 ======
  async loadLogs() {
    this.setData({ loading: true });
    var params = {};
    if (this.data.logType !== 'all') params.type = this.data.logType;
    try { var list = await api.get('/api/admin/logs', params); this.setData({ logList: list || [], loading: false }); } catch(e) { this.setData({ loading: false }); }
  },
  onLogKeyword(e) { this.setData({ logKeyword: e.detail.value }); },
  switchLogType(e) {
    this.setData({ logType: e.currentTarget.dataset.type });
    this.loadLogs();
  },
  async searchLogs() {
    var kw = this.data.logKeyword.trim();
    this.setData({ loading: true });
    var params = { keyword: kw };
    if (this.data.logType !== 'all') params.type = this.data.logType;
    try { var list = await api.get('/api/admin/logs', params); this.setData({ logList: list || [], loading: false }); } catch(e) { this.setData({ loading: false }); }
  }
});
