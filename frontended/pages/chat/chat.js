const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    sessionId: '',
    otherUserId: '',
    otherUser: {},
    messages: [],
    inputText: '',
    wsConnected: false,
    userId: 0,
    otherOnline: false,
    scrollTop: 0,
    myAvatarUrl: '',
    myNickName: '',
    statusBarHeight: 0
  },

  onLoad(options) {
    var otherId = options.otherId;
    var userInfo = wx.getStorageSync('userInfo');
    var uid = userInfo ? parseInt(userInfo.userId) : 0;
    var windowInfo = wx.getWindowInfo();
    this.setData({
      otherUserId: otherId,
      userId: uid,
      myAvatarUrl: (userInfo && userInfo.avatarUrl) || '',
      myNickName: (userInfo && userInfo.nickName) || '',
      statusBarHeight: (windowInfo && windowInfo.statusBarHeight) || 20
    });
    this.initChat(otherId, uid);
  },

  onShow() {
    this.checkOnline();
  },

  onUnload() {
    if (this.socketTask) {
      this.socketTask.close({ code: 1000, reason: 'page unload' });
    }
  },

  async initChat(otherId, uid) {
    try {
      var sessionId = await api.get('/api/chat/session/' + otherId);
      var results = await Promise.all([
        api.get('/api/user/simple/' + otherId),
        api.get('/api/chat/messages/' + sessionId)
      ]);
      var otherUser = results[0] || {};
      var rawMessages = results[1] || [];

      var messages = rawMessages.map(function(m) {
        m.isMine = parseInt(m.senderId) === uid;
        return m;
      });

      api.put('/api/chat/read/' + sessionId, {}).catch(function() {});

      this.setData({
        sessionId: sessionId,
        otherUser: otherUser,
        messages: messages
      }, function() {
        this.scrollToBottom();
      }.bind(this));

      this.connectWebSocket();
      this.checkOnline();
    } catch (e) {
      console.error('initChat error:', e);
    }
  },

  async checkOnline() {
    try {
      var online = await api.get('/api/chat/online/' + this.data.otherUserId);
      this.setData({ otherOnline: !!online });
    } catch (e) {}
  },

  connectWebSocket() {
    var uid = this.data.userId;
    if (!uid) return;

    var token = wx.getStorageSync('token');
    var wsUrl = app.globalData.wsUrl + '/' + uid;
    if (token) {
      wsUrl += '?token=' + encodeURIComponent(token);
    }
    this.socketTask = wx.connectSocket({ url: wsUrl });

    this.socketTask.onOpen(function() {
      this.setData({ wsConnected: true });
      this.checkOnline();
    }.bind(this));

    this.socketTask.onMessage(function(res) {
      try {
        var msg = JSON.parse(res.data);
        var otherId = parseInt(this.data.otherUserId);
        var senderId = parseInt(msg.senderId);

        if (senderId === otherId) {
          var newMsg = {
            senderId: senderId,
            content: msg.content,
            msgType: msg.msgType || 0,
            createTime: msg.createTime || '',
            isMine: false
          };
          this.setData({
            messages: this.data.messages.concat([newMsg])
          }, function() {
            this.scrollToBottom();
          }.bind(this));
          api.put('/api/chat/read/' + this.data.sessionId, {}).catch(function() {});
        }
      } catch (e) {}
    }.bind(this));

    this.socketTask.onClose(function() {
      this.setData({ wsConnected: false, otherOnline: false });
    }.bind(this));

    this.socketTask.onError(function() {
      this.setData({ wsConnected: false });
    }.bind(this));
  },

  sendMessage() {
    var text = (this.data.inputText || '').trim();
    if (!text) return;

    var msg = {
      senderId: this.data.userId,
      content: text,
      msgType: 0,
      createTime: new Date().toISOString(),
      isMine: true
    };

    this.setData({
      messages: this.data.messages.concat([msg]),
      inputText: ''
    }, function() {
      this.scrollToBottom();
    }.bind(this));

    this._send(text, 0);
  },

  sendImage() {
    var self = this;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: function(res) {
        wx.showLoading({ title: '发送中...' });
        api.uploadFile(res.tempFiles[0].tempFilePath).then(function(url) {
          wx.hideLoading();
          var msg = {
            senderId: self.data.userId,
            content: url,
            msgType: 1,
            createTime: new Date().toISOString(),
            isMine: true
          };
          self.setData({
            messages: self.data.messages.concat([msg])
          }, function() {
            self.scrollToBottom();
          });
          self._send(url, 1);
        }).catch(function() {
          wx.hideLoading();
          wx.showToast({ title: '上传失败', icon: 'none' });
        });
      }
    });
  },

  _send(content, msgType) {
    var otherId = parseInt(this.data.otherUserId);

    if (this.socketTask && this.data.wsConnected) {
      this.socketTask.send({
        data: JSON.stringify({ receiverId: otherId, content: content, msgType: msgType }),
        fail: function() {
          api.post('/api/chat/send', { receiverId: otherId, content: content, msgType: msgType }).catch(function() {});
        }
      });
    } else {
      api.post('/api/chat/send', { receiverId: otherId, content: content, msgType: msgType }).catch(function() {});
    }
  },

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  scrollToBottom() {
    this.setData({ scrollTop: 999999 });
  },

  previewMsgImage(e) {
    var url = e.currentTarget.dataset.url;
    wx.previewImage({ urls: [url], current: url });
  },

  goBack() {
    wx.navigateBack();
  },

  onOtherAvatarError() {
    this.setData({ 'otherUser._avatarError': true });
  },

  onMyAvatarError() {
    this.setData({ myAvatarError: true });
  },

  showOtherUserInfo() {
    var other = this.data.otherUser;
    var self = this;
    wx.showActionSheet({
      itemList: ['查看用户信息', '举报该用户'],
      success: function(res) {
        if (res.tapIndex === 0) {
          wx.showModal({
            title: other.nickName || '用户',
            content: '用户ID: ' + self.data.otherUserId,
            showCancel: false
          });
        } else if (res.tapIndex === 1) {
          wx.setStorageSync('reportContext', {
            type: 'user',
            id: parseInt(self.data.otherUserId),
            title: (other.nickName || '用户') + ' (ID:' + self.data.otherUserId + ')'
          });
          wx.navigateTo({ url: '/pages/report/publish/publish' });
        }
      }
    });
  }
});
