const api = require('../../utils/api');
const app = getApp();

Page({
  data: { list:[], tab:0, loading:true },

  onLoad(){ this.loadData(); },

  switchTab(e){
    this.setData({ tab:parseInt(e.currentTarget.dataset.tab), list:[], loading:true });
    this.loadData();
  },

  async loadData(){
    this.setData({ loading:true });
    var tab = this.data.tab;
    try {
      var raw;
      if (tab === 0) raw = await api.get('/api/evaluation/top-users');
      else if (tab === 1) raw = await api.get('/api/evaluation/top-teams');
      else raw = await api.get('/api/evaluation/top-clubs');
      var list = await this.enrichList(raw||[], tab);
      this.setData({ list, loading:false });
    } catch(e){
      this.setData({ list:[], loading:false });
    }
  },

  async enrichList(arr, tab){
    var userTasks = {};
    if (tab === 0) {
      // 并行拉取所有用户信息
      arr.forEach(function(item) {
        userTasks[item.target_id] = api.get('/api/user/simple/' + item.target_id).catch(function(){ return null; });
      });
    }
    var users = {};
    if (tab === 0) {
      var ids = Object.keys(userTasks);
      var results = await Promise.all(Object.values(userTasks));
      ids.forEach(function(id, i) { users[id] = results[i]; });
    }
    return (arr||[]).map(function(item) {
      if(tab === 0){
        item._score = Number(item.avg_score).toFixed(1);
        item._subtitle = '评价次数: ' + (item.count||0);
        var u = users[item.target_id];
        item._displayName = u ? (u.nickName||'用户') : '用户';
        item._avatarUrl = u ? (u.avatarUrl||'') : '';
      } else if(tab === 1){
        item._score = Number(item.avg_score).toFixed(1);
        item._subtitle = (item.count||0) + ' 次活动';
        item._displayName = item.title || '组局';
        item._avatarUrl = '';
      } else {
        item._score = Number(item.avg_score).toFixed(1);
        item._subtitle = (item.count||0) + ' 次活动';
        item._displayName = item.club_name || '社团';
        item._avatarUrl = '';
      }
      return item;
    });
  },

  goToDetail(e){
    var item = this.data.list[e.currentTarget.dataset.index];
    var tab = this.data.tab;
    if(tab === 0 && item.target_id){
      wx.navigateTo({ url: '/pages/user/user?userId=' + item.target_id });
    } else if(tab === 1 && item.team_id){
      wx.navigateTo({ url: '/pages/team/detail/detail?id=' + item.team_id });
    }
  }
});
