const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    title: '',
    description: '',
    tagIndex: 0,
    tags: ['学习', '运动', '旅游', '游戏', '社团', '恋爱', '其他', '自定义'],
    customTag: '',
    maxMembers: '10',
    minMembers: '1',
    location: '',
    startDate: '', startTime: '',
    endDate: '', endTime: '',
    publishing: false,
    isEdit: false,
    teamId: null,
    posterUrl: ''
  },

  onShow() {
    var url = wx.getStorageSync('posterResult');
    if (url) { this.setData({ posterUrl: url }); wx.removeStorageSync('posterResult'); }
  },

  onLoad(options) {
    if (options.teamId) {
      this.setData({ isEdit: true, teamId: parseInt(options.teamId) });
      wx.setNavigationBarTitle({ title: '编辑组局' });
      this.loadTeam(options.teamId);
    } else {
      // 新建组局：开始时间默认当前日期时分
      var now = new Date();
      var pad = function(n) { return n < 10 ? '0' + n : '' + n; };
      var today = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate());
      var time = pad(now.getHours()) + ':' + pad(now.getMinutes());
      this.setData({ startDate: today, startTime: time, today: today });
    }
  },

  async loadTeam(id) {
    wx.showLoading({ title: '加载中...' });
    try {
      var team = await api.get('/api/team/detail/' + id);
      var tagIdx = this.data.tags.indexOf(team.tag);
      if (tagIdx < 0) { tagIdx = this.data.tags.length - 1; }
      var sd = '', st = '', ed = '', et = '';
      if (team.startTime) { sd = team.startTime.substring(0, 10); st = team.startTime.substring(11, 16); }
      if (team.endTime) { ed = team.endTime.substring(0, 10); et = team.endTime.substring(11, 16); }
      this.setData({
        title: team.title || '',
        description: team.description || '',
        tagIndex: tagIdx,
        customTag: tagIdx === this.data.tags.length - 1 ? team.tag : '',
        maxMembers: String(team.maxMembers || 10),
        minMembers: String(team.minMembers || 1),
        location: team.location || '',
        startDate: sd, startTime: st, endDate: ed, endTime: et
      });
    } catch(e) { wx.showToast({ title: '加载失败', icon: 'none' }); }
    wx.hideLoading();
  },

  onTitleInput(e) { this.setData({ title: e.detail.value }); },
  onDescInput(e) { this.setData({ description: e.detail.value }); },
  onTagChange(e) {
    var idx = parseInt(e.detail.value);
    this.setData({ tagIndex: idx });
    if (idx < 7) this.setData({ customTag: '' });
  },
  onCustomTagInput(e) { this.setData({ customTag: e.detail.value }); },
  onMaxInput(e) { this.setData({ maxMembers: e.detail.value }); },
  onMinInput(e) { this.setData({ minMembers: e.detail.value }); },
  onLocationInput(e) { this.setData({ location: e.detail.value }); },
  onStartDateChange(e) { this.setData({ startDate: e.detail.value }); },
  onStartTimeChange(e) { this.setData({ startTime: e.detail.value }); },
  onEndDateChange(e) {
    var endDate = e.detail.value;
    // 如果与开始日期相同且结束时间早于开始时间，清空结束时间
    if (this.data.startDate && endDate === this.data.startDate && this.data.startTime && this.data.endTime && this.data.endTime < this.data.startTime) {
      this.setData({ endTime: '' });
    }
    this.setData({ endDate: endDate });
  },
  onEndTimeChange(e) {
    var endTime = e.detail.value;
    if (this.data.startDate && this.data.endDate === this.data.startDate && this.data.startTime && endTime <= this.data.startTime) {
      wx.showToast({ title: '结束时间不能早于或等于开始时间', icon: 'none' });
      return;
    }
    this.setData({ endTime: endTime });
  },

  async generateAI() {
    if (!this.data.title.trim()) { wx.showToast({ title:'请先填写标题', icon:'none' }); return; }
    wx.showLoading({ title:'AI生成中...' });
    try {
      var text = await api.post('/api/ai/generate', {
        type: 'team', title: this.data.title,
        category: this.data.customTag || this.data.tags[this.data.tagIndex],
        price: '0', description: this.data.description
      });
      this.setData({ description: text });
    } catch(e) {}
    wx.hideLoading();
  },

  async submit() {
    if (!this.data.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' }); return;
    }
    // 校验结束时间不能早于开始时间
    if (this.data.startDate && this.data.endDate) {
      var start = this.data.startDate + ' ' + (this.data.startTime || '00:00');
      var end = this.data.endDate + ' ' + (this.data.endTime || '00:00');
      if (end <= start) {
        wx.showToast({ title: '结束时间不能早于或等于开始时间', icon: 'none' }); return;
      }
    }
    this.setData({ publishing: true });
    try {
      var body = {
        title: this.data.title,
        description: this.data.description,
        tag: this.data.tagIndex === this.data.tags.length - 1 ? this.data.customTag : this.data.tags[this.data.tagIndex],
        maxMembers: parseInt(this.data.maxMembers) || 10,
        minMembers: parseInt(this.data.minMembers) || 1,
        location: this.data.location
      };
      if (this.data.startDate) body.startTime = this.data.startDate + ' ' + (this.data.startTime || '00:00') + ':00';
      if (this.data.endDate) body.endTime = this.data.endDate + ' ' + (this.data.endTime || '00:00') + ':00';
      if (this.data.posterUrl) body.image = this.data.posterUrl;
      if (this.data.isEdit) {
        await api.put('/api/team/update/' + this.data.teamId, body);
        wx.showToast({ title: '修改成功', icon: 'success' });
      } else {
        await api.post('/api/team/create', body);
        wx.showToast({ title: '发布成功', icon: 'success' });
      }
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (e) {}
    this.setData({ publishing: false });
  },

  designPoster() {
    wx.navigateTo({
      url: '/pages/poster/create/create?title=' + encodeURIComponent(this.data.title || '精彩活动')
        + '&location=' + encodeURIComponent(this.data.location || '')
    });
  }
});
