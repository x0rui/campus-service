const api = require('../../../utils/api');
Page({
  data:{type:0,clubName:'',title:'',content:'',publishing:false,posterUrl:''},
  onShow() {
    var url = wx.getStorageSync('posterResult');
    if (url) { this.setData({ posterUrl: url }); wx.removeStorageSync('posterResult'); }
  },
  setType(e){this.setData({type:parseInt(e.currentTarget.dataset.type)});},
  onClubInput(e){this.setData({clubName:e.detail.value});},
  onTitleInput(e){this.setData({title:e.detail.value});},
  onContentInput(e){this.setData({content:e.detail.value});},
  designPoster() {
    wx.navigateTo({
      url: '/pages/poster/create/create?title=' + encodeURIComponent(this.data.title || '校园公告')
        + '&location=' + encodeURIComponent(this.data.clubName || '')
    });
  },
  async submit(){
    if(!this.data.title.trim()){wx.showToast({title:'请输入标题',icon:'none'});return;}
    this.setData({publishing:true});
    try{
      var body = {clubName:this.data.clubName,title:this.data.title,content:this.data.content,type:this.data.type};
      if (this.data.posterUrl) body.image = this.data.posterUrl;
      await api.post('/api/announcement/create', body);
      wx.showToast({title:'发布成功',icon:'success'});
      setTimeout(function(){wx.navigateBack();},1000);
    }catch(e){}
    this.setData({publishing:false});
  }
});
