const api = require('../../../utils/api');
const util = require('../../../utils/util');
const app = getApp();

Page({
  data: {
    goods: null,
    seller: null,
    images: [],
    loading: true,
    canEvaluate: false,
    _timer: null
  },

  onLoad(options) {
    const id = options.id;
    const userInfo = wx.getStorageSync('userInfo');
    this.setData({ userInfo: userInfo || {} });
    if (id) this.loadDetail(id);
    this.startPolling();
  },
  onShow() { if (this.data.goods) { this.loadDetail(this.data.goods.goodsId); this.startPolling(); } },
  onHide(){ this.stopPolling(); },
  onUnload(){ this.stopPolling(); },
  startPolling(){ this.stopPolling(); var s=this; s.data._timer=setInterval(function(){ if(s.data.goods) s.loadDetail(s.data.goods.goodsId, true); },3000); },
  stopPolling(){ if(this.data._timer){clearInterval(this.data._timer);this.data._timer=null;} },

  async loadDetail(id, fromPoll) {
    if (this.data._loading) return;
    if (!fromPoll) this.setData({ loading: true });
    this.setData({ _loading: true });
    try {
      var url = '/api/goods/detail/' + id;
      if (fromPoll) url += '?poll=1';
      const goods = await api.get(url);
      let images = [];
      try {
        images = typeof goods.images === 'string' ? JSON.parse(goods.images) : (goods.images || []);
      } catch (e) {}
      this.setData({ goods, images, loading: false, _loading: false });

      if (goods.userId) {
        const seller = await api.get('/api/user/simple/' + goods.userId);
        this.setData({ seller });
      }

      // 如果是买家且还未评价
      var userInfo = wx.getStorageSync('userInfo');
      if (userInfo && goods.buyerId && goods.buyerId == userInfo.userId && goods.status === 1) {
        try {
          var evaluated = await api.get('/api/evaluation/check', { orderId: goods.goodsId, orderType: 1 });
          if (!evaluated) {
            this.setData({ canEvaluate: true });
          }
        } catch (e) {}
      }
    } catch (e) {
      this.setData({ loading: false, _loading: false });
    }
  },

  previewImage(e) {
    const idx = e.currentTarget.dataset.index;
    wx.previewImage({ urls: this.data.images, current: this.data.images[idx] });
  },

  async contactSeller() {
    if (!app.checkLogin()) return;
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo.userId === this.data.goods.userId) {
      wx.showToast({ title: '不能和自己交易', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: '/pages/chat/chat?otherId=' + this.data.goods.userId + '&goodsId=' + this.data.goods.goodsId
    });
  },

  editGoods() {
    wx.navigateTo({
      url: '/pages/goods/publish/publish?goodsId=' + this.data.goods.goodsId
    });
  },

  // 立即购买：先下单拿订单号，再进自绘支付页
  async buyNow() {
    if (!app.checkLogin()) return;
    try {
      const res = await api.post('/api/pay/unified-order', { goodsId: this.data.goods.goodsId });
      wx.navigateTo({
        url: '/pages/order/pay/pay?orderId=' + res.order.orderId +
             '&orderNo=' + res.order.orderNo + '&amount=' + res.order.amount
      });
    } catch (e) {}
  },

  async markAsSold() {
    const goods = this.data.goods;
    const res = await new Promise(r => {
      wx.showModal({ title: '提示', content: '确认标记为已售出？', success: r });
    });
    if (res.confirm) {
      wx.showModal({
        title: '输入买家ID',
        content: '',
        editable: true,
        placeholderText: '在聊天中点对方头像查看ID，填入这里',
        success: async (r2) => {
          if (r2.confirm) {
            const buyerId = parseInt(r2.content);
            if (!buyerId || isNaN(buyerId)) {
              wx.showToast({ title: '请输入有效买家ID', icon: 'none' });
              return;
            }
            try {
              await api.put('/api/goods/status/' + goods.goodsId, { status: 1, buyerId: buyerId });
              wx.showToast({ title: '已售出', icon: 'success' });
              setTimeout(() => {
                wx.navigateTo({
                  url: '/pages/evaluation/evaluation?targetId=' + buyerId + '&orderId=' + goods.goodsId + '&orderType=1'
                });
              }, 800);
            } catch (e) {
              wx.showToast({ title: '操作失败', icon: 'none' });
            }
          }
        }
      });
    }
  },

  evaluateSeller() {
    wx.navigateTo({
      url: '/pages/evaluation/evaluation?targetId=' + this.data.goods.userId + '&orderId=' + this.data.goods.goodsId + '&orderType=1'
    });
  },

  async offShelf() {
    const res = await new Promise(r => {
      wx.showModal({ title: '提示', content: '确认下架该物品？', success: r });
    });
    if (res.confirm) {
      try {
        await api.put('/api/goods/status/' + this.data.goods.goodsId, { status: 2 });
        wx.showToast({ title: '已下架', icon: 'success' });
        setTimeout(() => wx.navigateBack(), 1000);
      } catch (e) {
        wx.showToast({ title: '操作失败', icon: 'none' });
      }
    }
  },

  onSellerAvatarError() {
    var seller = this.data.seller;
    if (seller) {
      seller._avatarError = true;
      this.setData({ seller: seller });
    }
  },

  doReport() {
    var goods = this.data.goods;
    wx.setStorageSync('reportContext', { type: 'goods', id: goods.goodsId, title: goods.title || '' });
    wx.navigateTo({ url: '/pages/report/publish/publish' });
  }
});
