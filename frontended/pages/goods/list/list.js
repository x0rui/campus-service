const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    goodsList: [],
    page: 0,
    hasMore: true,
    keyword: '',
    activeCategory: '',
    categories: ['全部', '电子产品', '书籍教材', '生活用品', '服饰鞋包', '运动健身', '数码配件', '其他'],
    loading: false
  },

  onLoad() {
    // 先检查有没有登录，没登录跳登录页
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.loadGoods(); // 加载商品列表
  },

  onShow() {
    if (app.globalData.token) {
      var self = this;
      self.setData({ page: 0, hasMore: true });
      api.get('/api/goods/list', { page: 0 }).then(function(goods) {
        var list = self.parseImages(goods || []);
        self.setData({ goodsList: list, page: 1, hasMore: goods.length >= 10 });
      }).catch(function(){});
    }
  },

  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },

  parseImages(list) {
    return (list || []).map(item => {
      let imgs = [];
      if (item.images) {
        try { imgs = JSON.parse(item.images); } catch (e) {}
      }
      return Object.assign({}, item, { images: imgs, firstImage: imgs[0] || '' });
    });
  },

  async loadGoods() {
    if (this.data.loading || !this.data.hasMore) return;
    this.setData({ loading: true });
    try {
      // 调后端 GET /api/goods/list?page=0
      const goods = await api.get('/api/goods/list', { page: this.data.page });
      if (goods && goods.length > 0) {
        // 解析图片（images 字段是 JSON 字符串，转成数组取第一张）
        const parsed = this.parseImages(goods);

        // 追加到列表，翻页
        this.setData({
          goodsList: this.data.page === 0 ? parsed : [...this.data.goodsList, ...parsed],
          page: this.data.page + 1,
          hasMore: goods.length >= 10
        });
      } else {
        this.setData({ hasMore: false });
      }
    } catch (e) {}
    this.setData({ loading: false });
  },

  onSearch() {
    if (!this.data.keyword.trim()) {
      this.setData({ page: 0, goodsList: [], hasMore: true, activeCategory: '' });
      this.loadGoods();
      return;
    }
    this.searchGoods();
  },

  async searchGoods() {
    this.setData({ loading: true, hasMore: false });
    try {
      const goods = await api.get('/api/goods/search', { keyword: this.data.keyword });
      this.setData({ goodsList: this.parseImages(goods || []) });
    } catch (e) {}
    this.setData({ loading: false });
  },

  async filterByCategory(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ activeCategory: category, page: 0, goodsList: [], hasMore: true, keyword: '' });
    if (category === '全部') {
      this.loadGoods();
      return;
    }
    this.setData({ loading: true, hasMore: false });
    try {
      const goods = await api.get('/api/goods/category/' + category);
      this.setData({ goodsList: this.parseImages(goods || []) });
    } catch (e) {}
    this.setData({ loading: false });
  },

  onReachBottom() {
    if (!this.data.keyword && !this.data.activeCategory) {
      this.loadGoods();
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 0, goodsList: [], hasMore: true });
    this.loadGoods().then(function() {
      wx.stopPullDownRefresh();
    });
  },

  goToDetail(e) {
    wx.navigateTo({ url: '/pages/goods/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  goToPublish() {
    if (!app.checkLogin()) return;
    wx.navigateTo({ url: '/pages/goods/publish/publish' });
  },

  goDemand() {
    wx.navigateTo({ url: '/pages/goods/demand/demand' });
  },
  goOrders() {
    if (!app.checkLogin()) return;
    wx.navigateTo({ url: '/pages/order/list/list' });
  },
  goMyGoods() {
    if (!app.checkLogin()) return;
    wx.navigateTo({ url: '/pages/user/my-goods/my-goods' });
  }
});
