const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    team: null,
    userInfo: null,
    isCreator: false,
    myCheckedIn: false,
    members: [],
    pendingJoins: [],
    signCode: '',
    signInput: '',
    _timer: null
  },

  onLoad(options) {
    var id = options.id;
    var userInfo = wx.getStorageSync('userInfo');
    this.setData({ userInfo: userInfo || {} });
    if (id) { this.setData({ teamId: id }); this.loadDetail(id); }
    this.startPolling();
  },
  onShow() { if (this.data.teamId) { this.loadDetail(this.data.teamId); this.startPolling(); } },
  onHide(){ this.stopPolling(); },
  onUnload(){ this.stopPolling(); },
  startPolling(){ this.stopPolling(); var s=this; s.data._timer=setInterval(function(){ s.loadDetail(s.data.teamId); },3000); },
  stopPolling(){ if(this.data._timer){clearInterval(this.data._timer);this.data._timer=null;} },

  onPullDownRefresh() {
    if (this.data.teamId) {
      this.loadDetail(this.data.teamId).then(function() { wx.stopPullDownRefresh(); });
    }
  },

  async loadDetail(id) {
    if (this.data._loading) return;
    this.setData({ _loading: true });
    try {
      var team = await api.get('/api/team/detail/' + id);
      var [members, pending] = await Promise.all([
        api.get('/api/team/members/' + id),
        team && team.userId === this.data.userInfo.userId
          ? api.get('/api/team/pending/' + id) : Promise.resolve([])
      ]);
      var enrichedMembers = await this.enrichUsers(members || []);
      var enrichedPending = await this.enrichUsers(pending || []);
      var self = this;
      var me = enrichedMembers.find(function(m) { return m.userId === self.data.userInfo.userId; });
      this.setData({
        team: team,
        members: enrichedMembers,
        pendingJoins: enrichedPending,
        isCreator: team && team.userId === this.data.userInfo.userId,
        myCheckedIn: me ? (me.checkedIn === 1) : false,
        signCode: team ? (team.signCode || '') : '',
        checkedInCount: enrichedMembers.filter(function(m){return m.checkedIn===1}).length,
        _loading: false
      });
    } catch (e) { this.setData({ _loading: false }); }
  },

  async enrichUsers(list) {
    // 并行拉取所有成员信息，避免逐个请求导致的慢速和轮询覆盖问题
    var self = this;
    var tasks = list.map(function(item, i) {
      return api.get('/api/user/simple/' + item.userId).then(function(user) {
        item._nickName = user ? (user.nickName || '用户') : '用户';
        item._hobbies = user ? (user.hobbies || '') : '';
        item._gender = user ? (user.gender || '') : '';
        item._age = user ? (user.age || '') : '';
        item._avatarUrl = user ? (user.avatarUrl || '') : '';
        item._college = user ? (user.college || '') : '';
        item._major = user ? (user.major || '') : '';
        if (user && user.age) {
          var age = parseInt(user.age);
          var enrollYear = new Date().getFullYear() - age + 6;
          var grade = new Date().getFullYear() - enrollYear + 1;
          item._grade = (grade>=1&&grade<=4) ? '大'+['一','二','三','四'][grade-1] : '';
        } else { item._grade = ''; }
        return item;
      }).catch(function() {
        item._nickName = '用户'; item._hobbies = ''; item._grade = ''; item._avatarUrl = '';
        return item;
      });
    });
    return Promise.all(tasks);
  },

  showMemberInfo(e) {
    var idx = e.currentTarget.dataset.index;
    var m = this.data.members[idx];
    if (!m) return;
    wx.showModal({
      title: m._nickName || '用户',
      content: '性别: ' + (m._gender||'未设置') + '\n年级: ' + (m._grade||'未设置') + '\n学院: ' + (m._college||'未设置') + '\n专业: ' + (m._major||'未设置') + '\n兴趣: ' + (m._hobbies||'未设置'),
      showCancel: false
    });
  },

  cancelTeam() {
    var self = this;
    wx.showModal({
      title: '确认取消',
      content: '确定取消该组局吗？',
      success: function(res) {
        if (res.confirm) {
          api.post('/api/team/cancel/' + self.data.team.teamId).then(function() {
            wx.showToast({ title: '已取消', icon: 'success' });
            setTimeout(function() { wx.navigateBack(); }, 1000);
          }).catch(function() {});
        }
      }
    });
  },

  onMemberAvatarError(e) {
    var idx = e.currentTarget.dataset.index;
    var key = 'members[' + idx + ']._avatarUrl';
    this.setData({ [key]: '' });
  },
  onEvalAvatarError(e) {
    var idx = e.currentTarget.dataset.index;
    var key = 'members[' + idx + ']._avatarUrl';
    this.setData({ [key]: '' });
  },

  kickMember(e) {
    var targetId = e.currentTarget.dataset.id;
    var self = this;
    wx.showModal({
      title: '确认踢出',
      content: '确定将该成员踢出组局吗？',
      success: function(res) {
        if (res.confirm) {
          api.post('/api/team/kick/' + self.data.team.teamId + '/' + targetId).then(function() {
            wx.showToast({ title: '已踢出', icon: 'success' });
            self.loadDetail(self.data.team.teamId);
          }).catch(function() {});
        }
      }
    });
  },

  async applyJoin() {
    try {
      await api.post('/api/team/apply/' + this.data.team.teamId);
      wx.showToast({ title: '申请已发送', icon: 'success' });
      // 刷新 tab badge
      api.get('/api/user/badge').then(function(c){ if(c&&c.pendingTeamJoins>0) wx.setTabBarBadge({index:4,text:String(c.pendingTeamJoins)}); }).catch(function(){});
    } catch (e) {}
  },

  async approveJoin(e) {
    var joinId = e.currentTarget.dataset.id;
    var approved = e.currentTarget.dataset.approved === 'true';
    try {
      await api.put('/api/team/approve/' + joinId, { approved: approved });
      wx.showToast({ title: approved ? '已通过' : '已拒绝', icon: 'success' });
      // 只更新本地数据，不重新加载全部
      var pendings = this.data.pendingJoins.filter(function(p) { return p.joinId !== joinId; });
      this.setData({ pendingJoins: pendings });
      if (approved) {
        var team = this.data.team;
        team.currentMembers = (team.currentMembers || 1) + 1;
        this.setData({ team: team });
        // 重新加载成员列表
        var members = await api.get('/api/team/members/' + this.data.team.teamId);
        var enriched = await this.enrichUsers(members || []);
        this.setData({ members: enriched });
      }
    } catch (e) {}
  },

  async generateCode() {
    try {
      var code = await api.post('/api/team/sign-code/' + this.data.team.teamId);
      this.setData({ signCode: code });
      wx.showToast({ title: '签到码: ' + code, icon: 'success' });
    } catch (e) {}
  },

  onSignInput(e) { this.setData({ signInput: e.detail.value }); },

  async checkIn() {
    if (!this.data.signInput) {
      wx.showToast({ title: '请输入签到码', icon: 'none' }); return;
    }
    try {
      await api.post('/api/team/checkin/' + this.data.team.teamId, { code: this.data.signInput });
      wx.showToast({ title: '签到成功', icon: 'success' });
      this.setData({ signInput: '' });
      this.loadDetail(this.data.team.teamId);
    } catch (e) {}
  },

  contactCreator() {
    wx.navigateTo({ url: '/pages/chat/chat?otherId=' + this.data.team.userId });
  },

  contactMember(e) {
    wx.navigateTo({ url: '/pages/chat/chat?otherId=' + e.currentTarget.dataset.id });
  },

  evaluateMember(e) {
    var targetId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/evaluation/evaluation?targetId=' + targetId + '&orderId=' + this.data.team.teamId + '&orderType=2'
    });
  },

  doReport() {
    var t = this.data.team;
    wx.setStorageSync('reportContext', { type: 'team', id: t.teamId, title: t.title || '' });
    wx.navigateTo({ url: '/pages/report/publish/publish' });
  },

  editTeam() {
    wx.navigateTo({ url: '/pages/team/publish/publish?teamId=' + this.data.team.teamId });
  }
});
