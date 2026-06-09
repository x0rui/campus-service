const api = require('../../../utils/api');
Page({
  data: { club:null, posts:[], adminName:'', members:[], pendings:[], isMember:0, joinApplied:false, isClubAdmin:false, _timer:null },

  onLoad(options){
    if(options.id) { this.data.clubId = options.id; this.loadDetail(options.id); }
    this.startPolling();
  },
  onShow(){
    if (this.data.clubId) this.loadDetail(this.data.clubId);
    this.startPolling();
  },
  onHide(){ this.stopPolling(); },
  onUnload(){ this.stopPolling(); },
  startPolling(){ this.stopPolling(); var s=this; s.data._timer=setInterval(function(){ if(s.data.clubId) s.loadDetail(s.data.clubId, true); },3000); },
  stopPolling(){ if(this.data._timer){clearInterval(this.data._timer);this.data._timer=null;} },

  async loadDetail(id, fromPoll){
    if (this.data._loading) return;
    if (fromPoll && !this.data.club) return;
    this.setData({ _loading: true });
    try{
      var club = await api.get('/api/club/detail/' + id);
      var posts = await api.get('/api/announcement/list', {type:1});
      var clubPosts = (posts||[]).filter(function(p){ return p.clubName === club.clubName; });
      var user = null;
      try { user = await api.get('/api/user/simple/' + club.userId); } catch(e){}
      var allMembers = await api.get('/api/club/members/' + id);
      var approved = (allMembers||[]).filter(function(m){ return m.status===1; });
      var pending = (allMembers||[]).filter(function(m){ return m.status===0; });
      // 如果创建者不在成员列表里，自动加上
      if (club && user) {
        var hasAdmin = approved.some(function(m){ return m.userId === club.userId; });
        if (!hasAdmin) {
          approved.unshift({ userId:club.userId, nickName:user.nickName||'管理员', avatarUrl:user.avatarUrl||'', role:1, status:1, college:user.college||'', gender:user.gender||'', hobbies:user.hobbies||'' });
        }
      }
      var curUser = wx.getStorageSync('userInfo')||{};
      var isClubAdmin = curUser.userId === club.userId;
      try {
        var isMem = await api.get('/api/club/check-member/' + id);
        this.setData({ isMember: isMem });
      } catch(e){}
      this.setData({ club, posts:clubPosts, adminName:user?user.nickName||'':'社团管理员',
        members:approved, pendings:isClubAdmin?pending:[], isClubAdmin:isClubAdmin, _loading: false });
    }catch(e){ this.setData({ _loading: false }); }
  },

  async joinClub(){
    try {
      await api.post('/api/club/join/' + this.data.club.appId);
      wx.showToast({ title: '申请已发送', icon:'success' });
      this.loadDetail(this.data.club.appId);
    } catch(e){ wx.showToast({ title: '操作失败', icon:'none' }); }
  },

  async approveMember(e){
    var id = e.currentTarget.dataset.id;
    var approved = e.currentTarget.dataset.approved === 'true';
    try {
      await api.put('/api/club/member/approve/' + id, {approved:approved});
      wx.showToast({ title: approved?'已通过':'已拒绝', icon:'success' });
      this.loadDetail(this.data.club.appId);
    } catch(e){}
  },

  async kickMember(e){
    var id = e.currentTarget.dataset.id;
    try {
      await api.post('/api/club/member/kick/' + id);
      wx.showToast({ title:'已踢出', icon:'success' });
      this.loadDetail(this.data.club.appId);
    } catch(e){}
  },

  goToPost(e){
    wx.navigateTo({ url:'/pages/campus-circle/detail/detail?id='+e.currentTarget.dataset.id });
  },

  rateClub(){
    wx.navigateTo({ url:'/pages/evaluation/evaluation?targetId=' + this.data.club.userId + '&orderId=' + this.data.club.appId + '&orderType=3' });
  }
});
