const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    tab: 0,
    list: [],
    loading: false,
    showForm: false,
    categories: ['不限', '电子产品', '书籍教材', '生活用品', '服饰鞋包', '运动健身', '数码配件', '其他'],
    catIndex: 0,
    form: { title: '', keyword: '', maxPrice: '', description: '' },
    matchId: null,
    matchList: [],
    submitting: false
  },

  onLoad() {
    this.load();
  },

  onShow() {
    if (!this.data.showForm) this.load();
  },

  async load() {
    this.setData({ loading: true });
    const url = this.data.tab === 0 ? '/api/goods-demand/list' : '/api/goods-demand/my';
    try {
      const list = (await api.get(url, { page: 0 })) || [];
      this.setData({ list, matchId: null, matchList: [] });
    } catch (e) {}
    this.setData({ loading: false });
  },

  switchTab(e) {
    this.setData({ tab: Number(e.currentTarget.dataset.tab) });
    this.load();
  },

  toggleForm() {
    if (!app.checkLogin()) return;
    this.setData({ showForm: !this.data.showForm });
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key;
    const form = this.data.form;
    form[key] = e.detail.value;
    this.setData({ form });
  },

  onCatChange(e) { this.setData({ catIndex: Number(e.detail.value) }); },

  async submit() {
    const f = this.data.form;
    if (!f.title.trim()) return wx.showToast({ title: '请填写求购标题', icon: 'none' });
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    try {
      const res = await api.post('/api/goods-demand/publish', {
        title: f.title.trim(),
        category: this.data.catIndex === 0 ? '' : this.data.categories[this.data.catIndex],
        keyword: f.keyword.trim(),
        maxPrice: f.maxPrice ? Number(f.maxPrice) : null,
        description: f.description
      });
      wx.showToast({ title: '已发布，匹配到' + (res.matchCount || 0) + '件', icon: 'none' });
      this.setData({ showForm: false, form: { title: '', keyword: '', maxPrice: '', description: '' }, catIndex: 0, tab: 1 });
      this.load();
    } catch (e) {}
    this.setData({ submitting: false });
  },

  // 点开一条求购，查看匹配到的在售物品
  async toggleMatch(e) {
    const id = e.currentTarget.dataset.id;
    if (this.data.matchId === id) {
      this.setData({ matchId: null, matchList: [] });
      return;
    }
    try {
      const list = (await api.get('/api/goods-demand/match/' + id)) || [];
      this.setData({ matchId: id, matchList: list });
    } catch (e) {}
  },

  goGoods(e) {
    wx.navigateTo({ url: '/pages/goods/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  async close(e) {
    const id = e.currentTarget.dataset.id;
    const self = this;
    wx.showModal({
      title: '关闭求购', content: '确定关闭这条求购吗？',
      success(res) {
        if (!res.confirm) return;
        api.put('/api/goods-demand/status/' + id, { status: 2 }).then(function () {
          wx.showToast({ title: '已关闭', icon: 'none' });
          self.load();
        }).catch(function () {});
      }
    });
  }
});
