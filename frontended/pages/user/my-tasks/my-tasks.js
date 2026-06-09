const api = require('../../../utils/api');
Page({
  data: { list:[], taskSubTab:0, taskCount:{pending:0,ongoing:0,done:0,cancelled:0}, published:[], taken:[], userId:0 },
  onLoad(){ this.setData({userId:(wx.getStorageSync('userInfo')||{}).userId||0}); this.loadData(); },
  onShow(){ this.loadData(); },
  loadData(){
    var self=this;
    api.get('/api/task/my-published').then(function(pub){
      return api.get('/api/task/my-taken').then(function(taken){
        var p=pub||[], t=taken||[], all=p.concat(t);
        // 标记角色
        var uid=self.data.userId;
        p.forEach(function(item){ item._role='发布'; });
        t.forEach(function(item){ item._role='接单'; });
        self.setData({ published:p, taken:t, taskCount:{pending:all.filter(x=>x.status===0).length, ongoing:all.filter(x=>x.status===1).length, done:all.filter(x=>x.status===2).length, cancelled:all.filter(x=>x.status===3).length} });
        self.switchSubTab({currentTarget:{dataset:{tab:self.data.taskSubTab}}});
      });
    }).catch(function(){});
  },
  onPullDownRefresh(){ this.loadData().then(function(){ wx.stopPullDownRefresh(); }); },
  switchSubTab(e){
    var tab=parseInt(e.currentTarget.dataset.tab);
    this.setData({ taskSubTab:tab, list:(this.data.published.concat(this.data.taken)).filter(function(t){return t.status===tab;}) });
  },
  goToDetail(e){ wx.navigateTo({ url:'/pages/task/detail/detail?id='+e.currentTarget.dataset.id }); }
});
