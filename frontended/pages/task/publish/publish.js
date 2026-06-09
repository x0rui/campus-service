const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    pickupLocation: '',
    pickupLat: null,
    pickupLng: null,
    deliveryLocation: '',
    deliveryLat: null,
    deliveryLng: null,
    fee: '',
    deadline: '',
    remark: '',
    taskTypeIndex: 3,
    taskTypes: ['取快递', '取外卖', '代买', '代办', '自定义'],
    customType: '',
    deadlineTime: '23:59',
    publishing: false,
    isEdit: false,
    taskId: null
  },

  onLoad(options) {
    var now = new Date();
    var pad = function(n) { return n < 10 ? '0' + n : '' + n; };
    this.setData({ today: now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) });
    if (options.taskId) {
      this.setData({ isEdit: true, taskId: options.taskId });
      this.loadTask(options.taskId);
    }
  },

  async loadTask(taskId) {
    wx.showLoading({ title: '加载中...' });
    try {
      const task = await api.get('/api/task/detail/' + taskId);
      const typeIndex = this.data.taskTypes.indexOf(task.taskType);
      this.setData({
        pickupLocation: task.pickupLocation || '',
        pickupLat: task.pickupLat || null,
        pickupLng: task.pickupLng || null,
        deliveryLocation: task.deliveryLocation || '',
        deliveryLat: task.deliveryLat || null,
        deliveryLng: task.deliveryLng || null,
        fee: task.fee ? String(task.fee) : '',
        deadline: task.deadline || '',
        remark: task.remark || '',
        taskTypeIndex: typeIndex >= 0 ? typeIndex : 3
      });
    } catch (e) {}
    wx.hideLoading();
  },

  // 先获取手机定位，再把坐标传给地图，确保地图打开时定位到当前位置
  choosePickup() {
    var self = this;
    wx.getLocation({ type: 'gcj02', success: function(loc) {
      wx.chooseLocation({ latitude: loc.latitude, longitude: loc.longitude,
        success: function(res) {
          self.setData({ pickupLocation: res.name || res.address, pickupLat: res.latitude, pickupLng: res.longitude });
        }
      });
    }, fail: function() {
      wx.chooseLocation({
        success: function(res) {
          self.setData({ pickupLocation: res.name || res.address, pickupLat: res.latitude, pickupLng: res.longitude });
        }
      });
    }});
  },

  chooseDelivery() {
    var self = this;
    wx.getLocation({ type: 'gcj02', success: function(loc) {
      wx.chooseLocation({ latitude: loc.latitude, longitude: loc.longitude,
        success: function(res) {
          self.setData({ deliveryLocation: res.name || res.address, deliveryLat: res.latitude, deliveryLng: res.longitude });
        }
      });
    }, fail: function() {
      wx.chooseLocation({
        success: function(res) {
          self.setData({ deliveryLocation: res.name || res.address, deliveryLat: res.latitude, deliveryLng: res.longitude });
        }
      });
    }});
  },

  onTypeChange(e) {
    var idx = parseInt(e.detail.value);
    this.setData({ taskTypeIndex: idx });
    if (idx < 4) this.setData({ customType: '' });
  },
  onCustomTypeInput(e) { this.setData({ customType: e.detail.value }); },
  onFeeInput(e) { this.setData({ fee: e.detail.value }); },
  onDeadlineChange(e) { this.setData({ deadline: e.detail.value }); },
  onDeadlineTimeChange(e) { this.setData({ deadlineTime: e.detail.value }); },
  onRemarkInput(e) { this.setData({ remark: e.detail.value }); },

  async generateAI() {
    if (!this.data.pickupLocation || !this.data.deliveryLocation) {
      wx.showToast({ title: '请先选择取件点和送达点', icon: 'none' }); return;
    }
    wx.showLoading({ title: 'AI生成中...' });
    try {
      var text = await api.post('/api/ai/generate', {
        type: 'task',
        title: this.data.pickupLocation + '→' + this.data.deliveryLocation,
        category: this.data.taskTypes[this.data.taskTypeIndex],
        price: this.data.fee || '0',
        description: this.data.remark
      });
      this.setData({ remark: text });
    } catch(e) {}
    wx.hideLoading();
  },

  async submit() {
    if (!app.checkVerified()) return;
    if (!this.data.pickupLocation || !this.data.deliveryLocation) {
      wx.showToast({ title: '请选择取件点和送达点', icon: 'none' }); return;
    }
    if (!this.data.fee || parseFloat(this.data.fee) <= 0) {
      wx.showToast({ title: '请输入跑腿费', icon: 'none' }); return;
    }
    if (parseFloat(this.data.fee) > 100) {
      wx.showToast({ title: '跑腿费不能超过100元', icon: 'none' }); return;
    }
    if (!this.data.deadline) {
      wx.showToast({ title: '请选择截止时间', icon: 'none' }); return;
    }

    this.setData({ publishing: true });
    try {
      const data = {
        pickupLocation: this.data.pickupLocation,
        pickupLat: this.data.pickupLat,
        pickupLng: this.data.pickupLng,
        deliveryLocation: this.data.deliveryLocation,
        deliveryLat: this.data.deliveryLat,
        deliveryLng: this.data.deliveryLng,
        fee: parseFloat(this.data.fee),
        deadline: this.data.deadline ? this.data.deadline + ' ' + (this.data.deadlineTime || '23:59') : null,
        remark: this.data.remark,
        taskType: this.data.taskTypeIndex === 4 ? this.data.customType : this.data.taskTypes[this.data.taskTypeIndex]
      };

      if (this.data.isEdit) {
        await api.put('/api/task/' + this.data.taskId, data);
        wx.showToast({ title: '修改成功', icon: 'success' });
      } else {
        await api.post('/api/task/publish', data);
        wx.showToast({ title: '发布成功', icon: 'success' });
      }
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (e) {}
    this.setData({ publishing: false });
  }
});
