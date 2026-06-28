const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    editNickName:'', editAvatar:'', editPhone:'', editCollege:'', editMajor:'',
    editClassName:'', editAge:'', editGender:'', genderOptions:['男','女','其他'],
    editCustomHobbies:[], editCustomHobby:'', saving:false
  },

  onLoad() {
    // 先从缓存读取，再从API拉取最新数据
    var u = wx.getStorageSync('userInfo') || {};
    this.applyUserData(u);
    var self = this;
    api.get('/api/user/info').then(function(res) {
      if (res && (res.user || res.nickName)) {
        var fresh = res.user || res;
        // 合并进缓存
        var cached = wx.getStorageSync('userInfo') || {};
        Object.assign(cached, fresh);
        wx.setStorageSync('userInfo', cached);
        getApp().globalData.userInfo = cached;
        self.applyUserData(cached);
      }
    }).catch(function(){});
  },

  applyUserData(u) {
    var hobbies = (u.hobbies||'').split(',').filter(function(h){return h.trim();});
    this.setData({
      editNickName: u.nickName||'', editAvatar: u.avatarUrl||'', editPhone: u.phone||'',
      editCollege: u.college||'', editMajor: u.major||'', editClassName: u.className||'',
      editAge: u.age?String(u.age):'', editGender: u.gender||'',
      editCustomHobbies: hobbies,
      _user: u
    });
  },

  chooseAvatar() {
    wx.chooseMedia({ count:1, mediaType:['image'],
      success: (res) => {
        wx.showLoading({ title:'上传中...' });
        api.uploadFile(res.tempFiles[0].tempFilePath).then(function(url){
          wx.hideLoading(); wx.showToast({ title:'上传成功', icon:'success' });
          this.setData({ editAvatar: url });
        }.bind(this)).catch(function(){ wx.hideLoading(); });
      }
    });
  },

  onWechatAvatar(e) {
    if (e.detail.avatarUrl) this.setData({ editAvatar: e.detail.avatarUrl });
  },

  onNickInput(e){ this.setData({ editNickName: e.detail.value }); },
  onPhoneInput(e){ this.setData({ editPhone: e.detail.value }); },
  onCollegeInput(e){ this.setData({ editCollege: e.detail.value }); },
  onMajorInput(e){ this.setData({ editMajor: e.detail.value }); },
  onClassInput(e){ this.setData({ editClassName: e.detail.value }); },
  onAgeInput(e){ this.setData({ editAge: e.detail.value }); },
  onGenderChange(e){ this.setData({ editGender: this.data.genderOptions[e.detail.value] }); },
  onHobbyInput(e){ this.setData({ editCustomHobby: e.detail.value }); },

  addHobby() {
    var t = (this.data.editCustomHobby||'').trim();
    if (!t || this.data.editCustomHobbies.indexOf(t) >= 0) return;
    var list = this.data.editCustomHobbies;
    list.push(t);
    this.setData({ editCustomHobbies: list, editCustomHobby: '' });
  },

  removeHobby(e) {
    var idx = e.currentTarget.dataset.index;
    var list = this.data.editCustomHobbies;
    list.splice(idx, 1);
    this.setData({ editCustomHobbies: list });
  },

  save() {
    if (!(this.data.editNickName||'').trim()) { wx.showToast({ title:'昵称不能为空', icon:'none' }); return; }
    this.setData({ saving: true });
    var hobbies = this.data.editCustomHobbies.join(',');
    api.put('/api/user/info', {
      nickName: this.data.editNickName, avatarUrl: this.data.editAvatar,
      phone: this.data.editPhone, college: this.data.editCollege,
      major: this.data.editMajor, className: this.data.editClassName,
      age: this.data.editAge, gender: this.data.editGender,
      hobbies: hobbies
    }).then(function(){
      var u = this.data._user;
      u.nickName = this.data.editNickName; u.avatarUrl = this.data.editAvatar;
      u.phone = this.data.editPhone; u.college = this.data.editCollege;
      u.major = this.data.editMajor; u.className = this.data.editClassName;
      u.age = this.data.editAge?parseInt(this.data.editAge):null;
      u.gender = this.data.editGender; u.hobbies = hobbies;
      wx.setStorageSync('userInfo', u);
      app.globalData.userInfo = u;
      wx.showToast({ title:'保存成功', icon:'success' });
      setTimeout(function(){ wx.navigateBack(); }, 1000);
    }.bind(this)).catch(function(){ wx.showToast({ title:'保存失败', icon:'none' }); });
    this.setData({ saving: false });
  }
});
