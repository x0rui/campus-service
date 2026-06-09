const api = require('../../../utils/api');
Page({
  data: { list:[], loading:true, _timer:null },
  onShow(){
    this.loadData();
    this.startPolling();
  },
  onHide(){ this.stopPolling(); },
  onUnload(){ this.stopPolling(); },
  startPolling(){ this.stopPolling(); var s=this; s.data._timer=setInterval(function(){ s.loadData(true); },3000); },
  stopPolling(){ if(this.data._timer){clearInterval(this.data._timer);this.data._timer=null;} },
  async loadData(fromPoll){
    if (!fromPoll) this.setData({ loading:true });
    try{ var list = await api.get('/api/club/square'); this.setData({ list:list||[], loading:false }); }
    catch(e){ if (!fromPoll) this.setData({ loading:false }); }
  },
  goToDetail(e){ wx.navigateTo({ url:'/pages/club/detail/detail?id='+e.currentTarget.dataset.id }); },
  onPullDownRefresh(){ this.loadData().then(function(){ wx.stopPullDownRefresh(); }); }
});
