const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    conversations: [],
    loading: true,
    userId: null,
    unreadNotifications: 0
  },

  onLoad() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({ userId: userInfo.userId });
    }
  },

  onShow() {
    if (this.data.userId) {
      this.loadConversations();
      this.loadBadge();
    } else {
      const userInfo = wx.getStorageSync('userInfo');
      if (userInfo) {
        this.setData({ userId: userInfo.userId }, () => {
          this.loadConversations();
          this.loadBadge();
        });
      }
    }
  },

  async loadBadge() {
    try {
      const badge = await api.get('/api/user/badge');
      if (badge) {
        this.setData({ unreadNotifications: badge.unreadNotifications || 0 });
      }
    } catch (e) {}
  },

  goNotifications() {
    wx.navigateTo({ url: '/pages/notification/notification' });
  },

  async loadConversations() {
    this.setData({ loading: true });
    try {
      const raw = await api.get('/api/chat/conversations');
      const list = (raw || []).map(function(item) {
        item._time = formatTime(item.lastTime);
        if (!item.lastMessage) {
          item._preview = '';
        } else if (item.lastMsgType === 1) {
          item._preview = '[图片]';
        } else {
          var text = item.lastMessage;
          item._preview = text.length > 25 ? text.substring(0, 25) + '...' : text;
        }
        return item;
      });
      this.setData({ conversations: list, loading: false });
      var totalUnread = 0;
      (list || []).forEach(function(item) {
        totalUnread += (item.unreadCount || 0);
      });
      if (totalUnread > 0) {
        wx.setTabBarBadge({ index: 3, text: String(totalUnread > 99 ? 99 : totalUnread) });
      } else {
        wx.removeTabBarBadge({ index: 3 });
      }
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  openChat(e) {
    var id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/chat/chat?otherId=' + id });
  },

  onPullDownRefresh() {
    var self = this;
    this.loadConversations().then(function() {
      wx.stopPullDownRefresh();
    });
  },

  onAvatarError(e) {
    var index = e.currentTarget.dataset.index;
    var key = 'conversations[' + index + ']._avatarError';
    this.setData({ [key]: true });
  }
});

function formatTime(dateStr) {
  if (!dateStr) return '';
  var d = new Date(dateStr.replace(/-/g, '/'));
  var now = new Date();
  var hh = String(d.getHours()).padStart(2, '0');
  var mm = String(d.getMinutes()).padStart(2, '0');
  var md = String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');

  if (d.toDateString() === now.toDateString()) return hh + ':' + mm;
  var yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (d.toDateString() === yesterday.toDateString()) return '昨天';
  if (d.getFullYear() === now.getFullYear()) return md;
  return d.getFullYear() + '-' + md;
}
