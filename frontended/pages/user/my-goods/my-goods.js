const api = require('../../../utils/api');
Page({
  data: { list:[], goodsSubTab:0, goodsCount:{onSale:0,sold:0,offShelf:0,bought:0}, allGoods:[], boughtGoods:[] },
  onLoad(){ this.loadData(); },
  onShow(){ this.loadData(); },
  loadData(){
    var self=this;
    api.get('/api/goods/my').then(function(goods){
      return api.get('/api/goods/bought').then(function(bought){
        var g=goods||[], b=bought||[];
        self.setData({ allGoods:g, boughtGoods:b, goodsCount:{onSale:g.filter(x=>x.status===0).length, sold:g.filter(x=>x.status===1).length, offShelf:g.filter(x=>x.status===2).length, bought:b.length} });
        self.switchSubTab({currentTarget:{dataset:{tab:0}}});
      });
    }).catch(function(){});
  },
  onPullDownRefresh(){ this.loadData().then(function(){ wx.stopPullDownRefresh(); }); },
  switchSubTab(e){
    var tab=parseInt(e.currentTarget.dataset.tab);
    var list=tab===0?this.data.allGoods.filter(g=>g.status===0):tab===1?this.data.allGoods.filter(g=>g.status===1):tab===2?this.data.allGoods.filter(g=>g.status===2):this.data.boughtGoods||[];
    this.setData({ goodsSubTab:tab, list:list });
  },
  goToDetail(e){ wx.navigateTo({ url:'/pages/goods/detail/detail?id='+e.currentTarget.dataset.id }); }
});
