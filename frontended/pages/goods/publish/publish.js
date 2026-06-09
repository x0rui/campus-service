const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    images: [],
    title: '',
    description: '',
    price: '',
    categoryIndex: 0,
    categories: ['电子产品', '书籍教材', '生活用品', '服饰鞋包', '运动健身', '数码配件', '其他'],
    publishing: false,
    isEdit: false,
    goodsId: null
  },

  onLoad(options) {
    if (options.goodsId) {
      this.setData({ isEdit: true, goodsId: options.goodsId });
      this.loadGoods(options.goodsId);
    }
  },

  async loadGoods(goodsId) {
    wx.showLoading({ title: '加载中...' });
    try {
      const goods = await api.get('/api/goods/detail/' + goodsId);
      let images = [];
      try {
        images = typeof goods.images === 'string' ? JSON.parse(goods.images) : (goods.images || []);
      } catch (e) {}
      const categoryIndex = this.data.categories.indexOf(goods.category);
      this.setData({
        title: goods.title || '',
        description: goods.description || '',
        price: goods.price ? String(goods.price) : '',
        images: images,
        categoryIndex: categoryIndex >= 0 ? categoryIndex : 0
      });
    } catch (e) {}
    wx.hideLoading();
  },

  chooseImage() {
    const max = 6 - this.data.images.length;
    if (max <= 0) {
      wx.showToast({ title: '最多6张图片', icon: 'none' });
      return;
    }
    wx.chooseMedia({
      count: max,
      mediaType: ['image'],
      success: async (res) => {
        wx.showLoading({ title: '上传中...' });
        const urls = [...this.data.images];
        for (const item of res.tempFiles) {
          try {
            const url = await api.uploadFile(item.tempFilePath);
            urls.push(url);
          } catch (e) {
            wx.showToast({ title: '图片上传失败，请重试', icon: 'none' });
          }
        }
        if (urls.length === 0) { wx.hideLoading(); return; }
        this.setData({ images: urls });
        wx.hideLoading();
      }
    });
  },

  removeImage(e) {
    const idx = e.currentTarget.dataset.index;
    const images = [...this.data.images];
    images.splice(idx, 1);
    this.setData({ images });
  },

  onTitleInput(e) { this.setData({ title: e.detail.value }); },
  onPriceInput(e) { this.setData({ price: e.detail.value }); },
  onDescInput(e) { this.setData({ description: e.detail.value }); },
  onCategoryChange(e) {
    const idx = parseInt(e.detail.value);
    this.setData({ categoryIndex: idx });
  },

  async generateAI() {
    if (!this.data.title) {
      wx.showToast({ title: '请先填写标题', icon: 'none' });
      return;
    }
    wx.showLoading({ title: 'AI生成中...' });
    try {
      const text = await api.post('/api/ai/generate', {
        type: 'goods',
        title: this.data.title,
        category: this.data.categories[this.data.categoryIndex],
        price: this.data.price || '0',
        description: this.data.description
      });
      this.setData({ description: text });
    } catch (e) {}
    wx.hideLoading();
  },

  async submit() {
    this.setData({ publishing: true });
    if (!app.checkVerified()) return;
    if (!this.data.images.length) {
      wx.showToast({ title: '请至少上传一张图片', icon: 'none' }); return;
    }
    if (!this.data.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' }); return;
    }
    if (!this.data.price || parseFloat(this.data.price) <= 0) {
      wx.showToast({ title: '请输入有效价格', icon: 'none' }); return;
    }

    try {
      await api.post('/api/goods/check-words', {
        title: this.data.title,
        description: this.data.description
      });
    } catch (e) {
      return;
    }

    try {
      const data = {
        title: this.data.title,
        description: this.data.description,
        price: parseFloat(this.data.price),
        category: this.data.categories[this.data.categoryIndex],
        images: JSON.stringify(this.data.images)
      };

      if (this.data.isEdit) {
        await api.put('/api/goods/' + this.data.goodsId, data);
        wx.showToast({ title: '修改成功', icon: 'success' });
      } else {
        await api.post('/api/goods/publish', data);
        wx.showToast({ title: '发布成功', icon: 'success' });
      }
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (e) {}
    this.setData({ publishing: false });
  }
});
