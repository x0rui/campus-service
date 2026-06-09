const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    userInfo: null,
    myGoods: [], boughtGoods: [], filteredGoods: [], filteredTasks: [],
    goodsCount: { onSale:0,sold:0,offShelf:0,bought:0 },
    taskCount: { pending:0,ongoing:0,done:0,cancelled:0 },
    myPublishedTasks: [], myTakenTasks: [], avgScore: 5.0,
    currentTab: 0, showContent: false, goodsSubTab: 0, taskSubTab: 0,
    showEdit: false, editNickName: '', editAvatar: '', editPhone: '',
    hobbyList: [],
    editCollege: '', editMajor: '', editClassName: '', editAge: '', editGender: '',
    genderOptions: ['男','女','其他'],
    editHobbies: [], editCustomHobby: '', editCustomHobbies: [],
    showVerify: false, verifyName: '', verifyStuId: '', verifyPhone: '',
    verifyCollege: '', verifyMajor: '', verifyClassName: '', verifyAge: '', verifyGender: '',
    badgePendingClubApps: 0, badgeHandledReports: 0, badgePendingClubChecks: 0, badgePendingReports: 0, badgePendingTeamJoins: 0,
    userClub: null
  },

  onShow() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo) { wx.navigateTo({ url: '/pages/login/login' }); return; }
    // 刷新用户信息（角色变化后不用重新登录）
    var self = this;
    api.get('/api/user/info').then(function(res) {
      if (res) {
        var fresh = res.user || res;
        var old = wx.getStorageSync('userInfo') || {};
        fresh.nickName = fresh.nickName || old.nickName;
        wx.setStorageSync('userInfo', fresh);
        app.globalData.userInfo = fresh;
        if (res.newToken) {
          wx.setStorageSync('token', res.newToken);
          app.globalData.token = res.newToken;
        }
        self.setData({ userInfo: fresh, hobbyList: [...new Set((fresh.hobbies||'').split(',').filter(function(h){return h.trim();}))] });
      }
    }).catch(function(){});
    this.setData({ userInfo, hobbyList: [...new Set((userInfo.hobbies||'').split(',').filter(function(h){return h.trim();}))] });
    this.initEditFields();
    this.loadAllData();
    this.loadBadges();
    this.loadMyClub();
  },

  initEditFields() {
    const u = this.data.userInfo || {};
    var hobbies = u.hobbies ? u.hobbies.split(',').filter(h=>h.trim()) : [];
    var preset = ['学习','运动','旅游','游戏','社团','恋爱','音乐','摄影'];
    var matched = hobbies.filter(function(h){return preset.indexOf(h)>=0;});
    var custom = hobbies.filter(function(h){return preset.indexOf(h)<0;});
    this.setData({
      editNickName: u.nickName||'', editAvatar: u.avatarUrl||'', editPhone: u.phone||'',
      editCollege: u.college||'', editMajor: u.major||'', editClassName: u.className||'',
      editAge: u.age ? String(u.age) : '', editGender: u.gender||'',
      editHobbies: matched, editCustomHobbies: custom, editCustomHobby: ''
    });
  },

  async loadAllData() {
    try {
      const [goods, pubTasks, takenTasks, avgScore, boughtGoods] = await Promise.all([
        api.get('/api/goods/my'), api.get('/api/task/my-published'), api.get('/api/task/my-taken'),
        api.get('/api/evaluation/avg-score/'+this.data.userInfo.userId).catch(()=>5.0),
        api.get('/api/goods/bought')
      ]);
      var allTasks = (pubTasks||[]).concat(takenTasks||[]);
      this.setData({
        myGoods: goods||[], boughtGoods: boughtGoods||[],
        filteredGoods: (goods||[]).filter(g=>g.status===0),
        goodsCount: {
          onSale: (goods||[]).filter(g=>g.status===0).length,
          sold: (goods||[]).filter(g=>g.status===1).length,
          offShelf: (goods||[]).filter(g=>g.status===2).length,
          bought: (boughtGoods||[]).length
        },
        myPublishedTasks: pubTasks||[], myTakenTasks: takenTasks||[],
        filteredTasks: (pubTasks||[]).filter(t=>t.status===0),
        taskCount: {
          pending: allTasks.filter(t=>t.status===0).length,
          ongoing: allTasks.filter(t=>t.status===1).length,
          done: allTasks.filter(t=>t.status===2).length,
          cancelled: allTasks.filter(t=>t.status===3).length
        },
        avgScore: avgScore||5.0
      });
    } catch(e) {}
  },

  async loadBadges() {
    try {
      var viewTime = wx.getStorageSync('reportsViewTime') || 0;
      var counts = await api.get('/api/user/badge', { reportsViewTime: viewTime || 0 });
      if (counts) {
        // 未读消息 tab 角标
        var unread = counts.unreadMessages || 0;
        if (unread > 0) {
          wx.setTabBarBadge({ index: 3, text: String(unread > 99 ? '99+' : unread) });
        } else {
          wx.removeTabBarBadge({ index: 3 });
        }
        // 存储到 data 用于页面显示红点
        this.setData({
          badgePendingClubApps: counts.pendingClubApps || 0,
          badgeHandledReports: counts.handledReports || 0,
          badgePendingClubChecks: counts.pendingClubChecks || 0,
          badgePendingReports: counts.pendingReports || 0,
          badgePendingTeamJoins: counts.pendingTeamJoins || 0
        });
      }
    } catch(e) {}
  },

  async loadMyClub() {
    try {
      var list = await api.get('/api/club/my-club');
      this.setData({ userClub: (list||[])[0] || null });
    } catch(e) { this.setData({ userClub: null }); }
  },

  switchTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    if (tab === 0) wx.navigateTo({ url: '/pages/user/my-goods/my-goods' });
    else if (tab === 1) wx.navigateTo({ url: '/pages/user/my-tasks/my-tasks' });
  },

  showEditProfile() {
    wx.navigateTo({ url: '/pages/user/edit/edit' });
  },
  onNickInput(e) { this.setData({ editNickName: e.detail.value }); },
  onPhoneInput(e) { this.setData({ editPhone: e.detail.value }); },
  onEditCollegeInput(e) { this.setData({ editCollege: e.detail.value }); },
  onEditMajorInput(e) { this.setData({ editMajor: e.detail.value }); },
  onEditClassNameInput(e) { this.setData({ editClassName: e.detail.value }); },
  onEditAgeInput(e) { this.setData({ editAge: e.detail.value }); },
  onEditGenderChange(e) { this.setData({ editGender: this.data.genderOptions[e.detail.value] }); },
  onVerifyNameInput(e) { this.setData({ verifyName: e.detail.value }); },
  onVerifyStuIdInput(e) { this.setData({ verifyStuId: e.detail.value }); },
  onVerifyPhoneInput(e) { this.setData({ verifyPhone: e.detail.value }); },
  onVerifyCollegeInput(e) { this.setData({ verifyCollege: e.detail.value }); },
  onVerifyMajorInput(e) { this.setData({ verifyMajor: e.detail.value }); },
  onVerifyClassNameInput(e) { this.setData({ verifyClassName: e.detail.value }); },
  onVerifyAgeInput(e) { this.setData({ verifyAge: e.detail.value }); },
  onVerifyGenderChange(e) { this.setData({ verifyGender: this.data.genderOptions[e.detail.value] }); },
  toggleHobby(e) {
    var h = e.currentTarget.dataset.hobby;
    var list = this.data.editHobbies;
    var idx = list.indexOf(h);
    if (idx>=0) list.splice(idx,1); else list.push(h);
    this.setData({ editHobbies: list });
  },
  onCustomHobbyInput(e) { this.setData({ editCustomHobby: e.detail.value }); },
  addCustomHobby() {
    var t = (this.data.editCustomHobby||'').trim();
    var all = this.data.editHobbies.concat(this.data.editCustomHobbies);
    if (!t||all.indexOf(t)>=0) return;
    var list = this.data.editCustomHobbies; list.push(t);
    this.setData({ editCustomHobbies: list, editCustomHobby: '' });
  },
  removeCustomHobby(e) {
    var list = this.data.editCustomHobbies; list.splice(e.currentTarget.dataset.index,1);
    this.setData({ editCustomHobbies: list });
  },

  chooseAvatar() {
    wx.chooseMedia({ count:1, mediaType:['image'],
      success: async (res) => {
        wx.showLoading({ title:'上传中...' });
        try {
          var url = await api.uploadFile(res.tempFiles[0].tempFilePath);
          this.setData({ editAvatar: url });
          wx.hideLoading(); wx.showToast({ title:'上传成功', icon:'success' });
        } catch(e) { wx.hideLoading(); }
      }
    });
  },
  onChooseWechatAvatar(e) {
    var url = e.detail.avatarUrl;
    if (url) { this.setData({ editAvatar: url }); wx.showToast({ title:'已选择微信头像', icon:'success' }); }
  },

  async saveProfile() {
    var hobbiesAll = this.data.editHobbies.concat(this.data.editCustomHobbies);
    if (!(this.data.editNickName||'').trim()) { wx.showToast({ title:'昵称不能为空', icon:'none' }); return; }
    wx.showLoading({ title:'保存中...' });
    try {
      await api.put('/api/user/info', {
        nickName: this.data.editNickName, avatarUrl: this.data.editAvatar,
        phone: this.data.editPhone, college: this.data.editCollege,
        major: this.data.editMajor, className: this.data.editClassName,
        age: this.data.editAge, gender: this.data.editGender,
        hobbies: hobbiesAll.join(',')
      });
      var u = wx.getStorageSync('userInfo');
      Object.assign(u, {
        nickName: this.data.editNickName, avatarUrl: this.data.editAvatar,
        phone: this.data.editPhone, college: this.data.editCollege,
        major: this.data.editMajor, className: this.data.editClassName,
        age: this.data.editAge ? parseInt(this.data.editAge) : null,
        gender: this.data.editGender, hobbies: hobbiesAll.join(',')
      });
      wx.setStorageSync('userInfo', u);
      app.globalData.userInfo = u;
      this.setData({ userInfo: u, hobbyList: hobbiesAll, showEdit: false });
      wx.hideLoading(); wx.showToast({ title:'保存成功', icon:'success' });
    } catch(e) { wx.hideLoading(); }
  },

  showVerifyForm() {
    wx.navigateTo({ url: '/pages/user/verify/verify' });
  },

  async submitVerify() {
    if (!(this.data.verifyName||'').trim()||!(this.data.verifyStuId||'').trim()||!(this.data.verifyPhone||'').trim()||!(this.data.verifyCollege||'').trim()) {
      wx.showToast({ title:'请填写完整信息', icon:'none' }); return;
    }
    wx.showLoading({ title:'认证中...' });
    try {
      await api.post('/api/user/bind', {
        realName: this.data.verifyName, studentId: this.data.verifyStuId, phone: this.data.verifyPhone,
        college: this.data.verifyCollege, major: this.data.verifyMajor, className: this.data.verifyClassName,
        age: this.data.verifyAge, gender: this.data.verifyGender
      });
      var u = wx.getStorageSync('userInfo');
      Object.assign(u, {
        realName: this.data.verifyName, studentId: this.data.verifyStuId, phone: this.data.verifyPhone,
        college: this.data.verifyCollege, major: this.data.verifyMajor, className: this.data.verifyClassName,
        age: this.data.verifyAge?parseInt(this.data.verifyAge):null, gender: this.data.verifyGender
      });
      wx.setStorageSync('userInfo', u);
      app.globalData.userInfo = u;
      this.setData({ userInfo: u, showVerify: false });
      wx.hideLoading(); wx.showToast({ title:'认证成功', icon:'success' });
    } catch(e) { wx.hideLoading(); }
  },

  switchGoodsSubTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    var filtered = tab===0?this.data.myGoods.filter(g=>g.status===0)
      : tab===1?this.data.myGoods.filter(g=>g.status===1)
      : tab===2?this.data.myGoods.filter(g=>g.status===2)
      : this.data.boughtGoods||[];
    this.setData({ goodsSubTab: tab, filteredGoods: filtered });
  },

  switchTaskSubTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    var all = (this.data.myPublishedTasks||[]).concat(this.data.myTakenTasks||[]);
    this.setData({ taskSubTab: tab, filteredTasks: all.filter(t=>t.status===tab) });
  },

  goToGoodsDetail(e) { wx.navigateTo({ url: '/pages/goods/detail/detail?id='+e.currentTarget.dataset.id }); },
  goToTaskDetail(e) { wx.navigateTo({ url: '/pages/task/detail/detail?id='+e.currentTarget.dataset.id }); },
  goToEvaluations() { wx.navigateTo({ url: '/pages/evaluation/evaluation-list' }); },
  goToMyTeams() { wx.navigateTo({ url: '/pages/team/my/my' }); },
  goToApplyClub() { wx.navigateTo({ url: '/pages/user/apply-club/apply-club' }); },
  goToMyApply() { wx.navigateTo({ url: '/pages/user/my-apply/my-apply' }); },
  goToCampusCircle() { wx.navigateTo({ url: '/pages/campus-circle/list/list' }); },
  goToMyReports() { wx.navigateTo({ url: '/pages/report/my/my' }); },
  goToClubSquare() { wx.navigateTo({ url: '/pages/club/square/square' }); },
  goToClubDetail() { if (this.data.userClub) wx.navigateTo({ url: '/pages/club/detail/detail?id=' + this.data.userClub.appId }); },
  goToAdmin() {
    var u = wx.getStorageSync('userInfo');
    if (u&&u.role===2) wx.navigateTo({ url: '/pages/admin/admin' });
    else wx.showToast({ title:'仅管理员可访问', icon:'none' });
  },

  logout() {
    wx.showModal({
      title:'提示', content:'确认退出登录？',
      success: function(res) {
        if (res.confirm) {
          wx.removeStorageSync('token'); wx.removeStorageSync('userInfo');
          app.globalData.token = ''; app.globalData.userInfo = null;
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});
