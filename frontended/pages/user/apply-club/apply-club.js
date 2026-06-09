const api = require('../../../utils/api');
Page({
  data: { clubName:'', description:'', submitting:false },
  onNameInput(e){ this.setData({ clubName: e.detail.value }); },
  onDescInput(e){ this.setData({ description: e.detail.value }); },
  submit() {
    if (!(this.data.clubName||'').trim()) { wx.showToast({ title:'请输入社团名称', icon:'none' }); return; }
    this.setData({ submitting: true });
    api.post('/api/club/apply', { clubName:this.data.clubName, description:this.data.description }).then(function(){
      wx.showToast({ title:'申请已提交', icon:'success' });
      setTimeout(function(){ wx.navigateBack(); }, 1000);
    }.bind(this)).catch(function(){});
    this.setData({ submitting: false });
  }
});
