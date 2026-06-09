const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    verifyName:'', verifyStuId:'', verifyPhone:'', verifyCollege:'', verifyMajor:'',
    verifyClassName:'', verifyAge:'', verifyGender:'', genderOptions:['男','女','其他'],
    cardFrontUrl:'', cardBackUrl:'',
    submitting: false
  },

  onLoad() {
    var u = wx.getStorageSync('userInfo') || {};
    if (u.realName) {
      this.setData({ verifyName: u.realName, verifyStuId: u.studentId, verifyPhone: u.phone,
        verifyCollege: u.college, verifyMajor: u.major, verifyClassName: u.className,
        verifyAge: u.age?String(u.age):'', verifyGender: u.gender||'' });
    }
  },

  chooseCardFront() {
    var self = this;
    wx.chooseMedia({ count:1, mediaType:['image'], sizeType:['compressed'],
      success: function(res) {
        wx.showLoading({ title:'上传中...' });
        api.uploadFile(res.tempFiles[0].tempFilePath).then(function(url) {
          self.setData({ cardFrontUrl: url });
          wx.hideLoading();
        }).catch(function() { wx.hideLoading(); wx.showToast({ title:'上传失败', icon:'none' }); });
      }
    });
  },

  chooseCardBack() {
    var self = this;
    wx.chooseMedia({ count:1, mediaType:['image'], sizeType:['compressed'],
      success: function(res) {
        wx.showLoading({ title:'上传中...' });
        api.uploadFile(res.tempFiles[0].tempFilePath).then(function(url) {
          self.setData({ cardBackUrl: url });
          wx.hideLoading();
        }).catch(function() { wx.hideLoading(); wx.showToast({ title:'上传失败', icon:'none' }); });
      }
    });
  },

  submit() {
    if (!(this.data.verifyName||'').trim()||!(this.data.verifyStuId||'').trim()||!(this.data.verifyPhone||'').trim()||!(this.data.verifyCollege||'').trim()) {
      wx.showToast({ title:'请填写完整信息', icon:'none' }); return;
    }
    this.setData({ submitting: true });
    api.post('/api/user/bind', {
      realName: this.data.verifyName, studentId: this.data.verifyStuId, phone: this.data.verifyPhone,
      college: this.data.verifyCollege, major: this.data.verifyMajor, className: this.data.verifyClassName,
      age: this.data.verifyAge, gender: this.data.verifyGender,
      cardFront: this.data.cardFrontUrl, cardBack: this.data.cardBackUrl
    }).then(function(){
      var u = wx.getStorageSync('userInfo');
      u.realName = this.data.verifyName; u.studentId = this.data.verifyStuId; u.phone = this.data.verifyPhone;
      u.college = this.data.verifyCollege; u.major = this.data.verifyMajor; u.className = this.data.verifyClassName;
      u.age = this.data.verifyAge?parseInt(this.data.verifyAge):null; u.gender = this.data.verifyGender;
      u.cardFront = this.data.cardFrontUrl; u.cardBack = this.data.cardBackUrl;
      wx.setStorageSync('userInfo', u);
      app.globalData.userInfo = u;
      wx.showToast({ title:'认证成功', icon:'success' });
      setTimeout(function(){ wx.navigateBack(); }, 1000);
    }.bind(this)).catch(function(){ wx.showToast({ title:'认证失败', icon:'none' }); });
    this.setData({ submitting: false });
  },

  onNameInput(e){ this.setData({ verifyName: e.detail.value }); },
  onStuIdInput(e){ this.setData({ verifyStuId: e.detail.value }); },
  onPhoneInput(e){ this.setData({ verifyPhone: e.detail.value }); },
  onCollegeInput(e){ this.setData({ verifyCollege: e.detail.value }); },
  onMajorInput(e){ this.setData({ verifyMajor: e.detail.value }); },
  onClassInput(e){ this.setData({ verifyClassName: e.detail.value }); },
  onAgeInput(e){ this.setData({ verifyAge: e.detail.value }); },
  onGenderChange(e){ this.setData({ verifyGender: this.data.genderOptions[e.detail.value] }); }
});
