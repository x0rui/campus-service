const api = require('../../../utils/api');
Page({
  data: { post:null, comments:[], threadReplies:{}, commentText:'', isOwner:false, myUserId:0, replyToId:0, replyToName:'', _timer:null },
  onLoad(options){
    this.data.postId = options.id;
    this.loadDetail(options.id);
    this.startPolling();
  },
  onShow(){
    if (this.data.postId) this.loadDetail(this.data.postId);
    this.startPolling();
  },
  onHide(){ this.stopPolling(); },
  onUnload(){ this.stopPolling(); },
  startPolling(){
    this.stopPolling();
    var self = this;
    self.data._timer = setInterval(function(){ self.loadDetail(self.data.postId); }, 3000);
  },
  stopPolling(){
    if (this.data._timer) { clearInterval(this.data._timer); this.data._timer = null; }
  },
  async loadDetail(id){
    try{
      var [post, comments] = await Promise.all([
        api.get('/api/announcement/detail/'+id),
        api.get('/api/announcement/comments/'+id)
      ]);
      var u = wx.getStorageSync('userInfo');
      // 抖音风格评论：顶层 + 扁平回复列表
      var topLevel = [], threadReplies = {}, replyMap = {};
      (comments||[]).forEach(function(c){ replyMap[c.id] = c; });
      (comments||[]).forEach(function(c){
        if (c.replyTo && c.replyTo > 0) {
          // 找顶层父评论ID
          var rootId = c.replyTo;
          var parent = replyMap[rootId];
          while (parent && parent.replyTo && parent.replyTo > 0) {
            rootId = parent.replyTo;
            parent = replyMap[rootId];
          }
          if (!threadReplies[rootId]) threadReplies[rootId] = [];
          // 标记回复谁
          var replyToName = replyMap[c.replyTo] ? (replyMap[c.replyTo].nickName || '用户') : '';
          c._replyToName = replyToName;
          threadReplies[rootId].push(c);
        } else {
          topLevel.push(c);
        }
      });
      this.setData({ post:post, comments:topLevel, threadReplies:threadReplies, isOwner:u&&post&&u.userId===post.userId, myUserId:u?u.userId:0 });
    }catch(e){}
  },
  async loadComments(id){
    if (!id) return;
    try{
      var comments = await api.get('/api/announcement/comments/' + id);
      var u = wx.getStorageSync('userInfo');
      var topLevel = [], threadReplies = {}, replyMap = {};
      (comments||[]).forEach(function(c){ replyMap[c.id] = c; });
      (comments||[]).forEach(function(c){
        if (c.replyTo && c.replyTo > 0) {
          var rootId = c.replyTo;
          var parent = replyMap[rootId];
          while (parent && parent.replyTo && parent.replyTo > 0) {
            rootId = parent.replyTo;
            parent = replyMap[rootId];
          }
          if (!threadReplies[rootId]) threadReplies[rootId] = [];
          var replyToName = replyMap[c.replyTo] ? (replyMap[c.replyTo].nickName || '') : '';
          c._replyToName = replyToName;
          threadReplies[rootId].push(c);
        } else {
          topLevel.push(c);
        }
      });
      this.setData({ comments:topLevel, threadReplies:threadReplies });
    }catch(e){}
  },
  onCommentInput(e){ this.setData({ commentText: e.detail.value }); },
  setReply(e){
    var id = parseInt(e.currentTarget.dataset.id);
    var name = e.currentTarget.dataset.name || '用户';
    this.setData({ replyToId: id, replyToName: name, commentText: '@'+name+' ' });
  },
  cancelReply(){
    this.setData({ replyToId:0, replyToName:'', commentText:'' });
  },
  async doComment(){
    if(!this.data.commentText.trim()) return;
    try{
      var body = {content:this.data.commentText};
      if (this.data.replyToId > 0) body.replyTo = this.data.replyToId;
      await api.post('/api/announcement/comment/'+this.data.post.id, body);
      wx.showToast({title:'已评论',icon:'success'});
      this.setData({commentText:'', replyToId:0, replyToName:''});
      this.loadDetail(this.data.post.id);
    }catch(e){}
  },
  async doLike(){
    try{
      var liked = await api.post('/api/announcement/like/'+this.data.post.id);
      var post = this.data.post;
      post.likeCount = Math.max(0, (post.likeCount||0) + (liked ? 1 : -1));
      this.setData({post:post});
      wx.showToast({title: liked ? '已点赞' : '已取消点赞', icon:'success'});
    }catch(e){}
  },
  async doCommentLike(e){
    var commentId = e.currentTarget.dataset.id;
    try{
      await api.post('/api/announcement/comment/like/' + commentId);
      this.loadDetail(this.data.postId);
    }catch(e){}
  },
  async doPin(){
    try{
      await api.post('/api/announcement/pin/'+this.data.post.id);
      wx.showToast({title:'操作成功',icon:'success'});
      this.loadDetail(this.data.post.id);
    }catch(e){}
  },
  async pinComment(e){
    var commentId = e.currentTarget.dataset.id;
    try{
      await api.post('/api/announcement/comment/pin/' + commentId + '?postId=' + this.data.post.id);
      wx.showToast({title:'操作成功',icon:'success'});
      this.loadDetail(this.data.post.id);
    }catch(e){}
  },
  doReport() {
    var post = this.data.post;
    wx.setStorageSync('reportContext', { type: post.type===0?'post':'announcement', id: post.id, title: post.title || '' });
    wx.navigateTo({ url: '/pages/report/publish/publish' });
  },
  async deleteComment(e) {
    var id = e.currentTarget.dataset.id;
    var self = this;
    wx.showModal({
      title: '确认删除', content: '确定删除这条评论吗？',
      success: async function(res) {
        if (res.confirm) {
          try {
            await api.del('/api/announcement/comment/' + id);
            wx.showToast({ title: '已删除', icon: 'success' });
            self.loadDetail(self.data.postId);
          } catch(e) {}
        }
      }
    });
  },
  async doDelete() {
    var self = this;
    wx.showModal({
      title: '确认删除', content: '确定删除这条帖子吗？删除后不可恢复。',
      success: async function(res) {
        if (res.confirm) {
          try {
            await api.del('/api/announcement/delete/' + self.data.post.id);
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(function() { wx.navigateBack(); }, 1000);
          } catch(e) {}
        }
      }
    });
  }
});
