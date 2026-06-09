const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    activeSection: 'recommend',
    recommendUsers: [],
    teamList: [],
    teamPage: 0,
    hasMore: true,
    activeTag: '',
    tags: ['全部','学习','运动','旅游','游戏','社团','恋爱','其他'],
    keyword: '',
    loading: true,
    _loaded: false
  },

  onLoad() {
    if (!wx.getStorageSync('token')) { wx.navigateTo({ url: '/pages/login/login' }); return; }
  },

  onShow() {
    if (app.globalData.token) { this.loadRecommends(); this.loadTeams(true); }
  },

  switchSection(e) {
    this.setData({ activeSection: e.currentTarget.dataset.section });
  },

  async loadRecommends() {
    try {
      var users = await api.get('/api/user/recommend');
      // 计算年级
      var currentYear = new Date().getFullYear();
      if (users) {
        users.forEach(function(u) {
          if (u.age) {
            var grade = currentYear - (currentYear - parseInt(u.age) + 6) + 1;
            u._grade = (grade>=1&&grade<=4) ? '大'+['一','二','三','四'][grade-1] : '';
          } else { u._grade = ''; }
        });
      }
      this.setData({ recommendUsers: users||[] });
    } catch(e) { this.setData({ recommendUsers: [] }); }
  },

  async loadTeams(reset) {
    if (reset) {
      this.setData({ teamPage: 0, teamList: [], hasMore: true, loading: true });
    } else {
      if (!this.data.hasMore || this.data.loading) return;
      this.setData({ loading: true });
    }
    try {
      var list = await api.get('/api/team/list', { page: this.data.teamPage });
      if (list && list.length > 0) {
        this.setData({ teamList: this.data.teamList.concat(list), teamPage: this.data.teamPage + 1, hasMore: list.length >= 10 });
      } else {
        this.setData({ hasMore: false });
      }
    } catch(e) {}
    this.setData({ loading: false });
  },

  onPullDownRefresh() {
    this.loadRecommends();
    if (this.data.activeTag && this.data.activeTag !== '全部') {
      this.setData({ teamPage: 0, teamList: [], hasMore: true, loading: true });
      api.get('/api/team/tag/' + this.data.activeTag).then(function(list) {
        this.setData({ teamList: list||[], hasMore: false, loading: false });
        wx.stopPullDownRefresh();
      }.bind(this)).catch(function(){ wx.stopPullDownRefresh(); });
    } else {
      this.loadTeams(true).then(function() { wx.stopPullDownRefresh(); });
    }
  },

  onReachBottom() {
    if (this.data.activeSection === 'team') this.loadTeams(false);
  },

  async filterByTag(e) {
    var tag = e.currentTarget.dataset.tag;
    this.setData({ activeTag: tag, teamPage: 0, teamList: [], hasMore: true });
    if (tag === '全部') { this.loadTeams(true); return; }
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/team/tag/' + tag);
      this.setData({ teamList: list||[], hasMore: false });
    } catch(e) {}
    this.setData({ loading: false });
  },

  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },
  async onSearch() {
    if (!this.data.keyword.trim()) { this.loadTeams(); return; }
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/team/search',{keyword:this.data.keyword});
      this.setData({ teamList: list||[], loading: false });
    } catch(e) { this.setData({ loading: false }); }
  },

  contactUser(e) { wx.navigateTo({ url: '/pages/chat/chat?otherId='+e.currentTarget.dataset.id }); },
  goToDetail(e) { wx.navigateTo({ url: '/pages/team/detail/detail?id='+e.currentTarget.dataset.id }); },
  goToPublish() { wx.navigateTo({ url: '/pages/team/publish/publish' }); },
  onAvatarError(e) {
    var idx = e.currentTarget.dataset.index;
    var key = 'recommendUsers['+idx+'].avatarUrl';
    this.setData({ [key]: '' });
  }
});
