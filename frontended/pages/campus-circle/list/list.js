const api = require('../../../utils/api');
Page({
  data: { list:[], activeType:0, page:0, hasMore:true, loading:false },
  onLoad(){ this.load(0, true); },
  onShow(){ this.load(this.data.activeType, true); },
  async load(type, reset){
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      var page = reset ? 0 : this.data.page;
      // type=2 是"推荐"页：按我的兴趣标签匹配帖子
      var list = (type === 2)
        ? await api.get('/api/announcement/recommend')
        : await api.get('/api/announcement/list', { type: type, page: page });
      list = list || [];
      // 标签是逗号分隔的字符串，拆成数组方便 wxml 渲染
      list = list.map(function (p) {
        p.tagList = (p.tags || '').split(',').filter(function (x) { return x && x.trim(); });
        return p;
      });
      if (reset) {
        this.setData({ list: list, page: 0, hasMore: list.length >= 10 });
      } else {
        this.setData({ list: this.data.list.concat(list), page: page + 1, hasMore: list.length >= 10 });
      }
    } catch(e) {}
    this.setData({ loading: false });
  },
  switchTab(e){
    var type = parseInt(e.currentTarget.dataset.type);
    this.setData({ activeType: type }); this.load(type, true);
  },
  goToDetail(e){ wx.navigateTo({url:'/pages/campus-circle/detail/detail?id='+e.currentTarget.dataset.id}); },
  goToPublish(){ wx.navigateTo({url:'/pages/campus-circle/publish/publish'}); },
  onPullDownRefresh(){
    var self = this;
    this.load(this.data.activeType, true).then(function(){ wx.stopPullDownRefresh(); });
  },
  onReachBottom(){
    if (this.data.hasMore && !this.data.loading) {
      this.load(this.data.activeType, false);
    }
  }
});
