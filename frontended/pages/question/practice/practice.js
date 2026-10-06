const api = require('../../../utils/api');

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F'];

Page({
  data: {
    subjects: ['全部科目'],
    subjectIndex: 0,
    subject: '',
    chapter: '',
    questions: [],
    idx: 0,
    current: null,
    letters: LETTERS,
    userAnswer: '',
    submitted: false,
    result: null,
    loading: false
  },

  onLoad() {
    if (!wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.loadSubjects();
    this.loadQuestions();
  },

  async loadSubjects() {
    try {
      const s = await api.get('/api/question/subjects');
      this.setData({ subjects: ['全部科目'].concat(s || []) });
    } catch (e) {}
  },

  async loadQuestions() {
    this.setData({ loading: true });
    try {
      const q = { page: 0 };
      if (this.data.subject) q.subject = this.data.subject;
      if (this.data.chapter.trim()) q.chapter = this.data.chapter.trim();
      const list = (await api.get('/api/question/list', q)) || [];
      const questions = list.map(item => {
        let opts = [];
        try { opts = JSON.parse(item.options || '[]'); } catch (e) {}
        return Object.assign({}, item, { optList: opts });
      });
      this.setData({ questions: questions, idx: 0 });
      this.showCurrent();
    } catch (e) {}
    this.setData({ loading: false });
  },

  showCurrent() {
    const qs = this.data.questions;
    this.setData({
      current: qs.length ? qs[this.data.idx] : null,
      userAnswer: '',
      submitted: false,
      result: null
    });
  },

  onSubjectChange(e) {
    const i = Number(e.detail.value);
    this.setData({ subjectIndex: i, subject: i === 0 ? '' : this.data.subjects[i] });
    this.loadQuestions();
  },

  onChapterInput(e) { this.setData({ chapter: e.detail.value }); },
  onSearch() { this.loadQuestions(); },

  pickOption(e) {
    if (this.data.submitted) return;
    const letter = e.currentTarget.dataset.letter;
    if (this.data.current.qType === 1) {
      // 多选：点一次加/减
      let cur = this.data.userAnswer.split('');
      if (cur.indexOf(letter) >= 0) cur = cur.filter(c => c !== letter);
      else cur.push(letter);
      cur.sort();
      this.setData({ userAnswer: cur.join('') });
    } else {
      this.setData({ userAnswer: letter });
    }
  },

  onInputAnswer(e) { this.setData({ userAnswer: e.detail.value }); },

  async submit() {
    if (this.data.submitted || !this.data.current) return;
    if (!this.data.userAnswer) return wx.showToast({ title: '请先作答', icon: 'none' });
    try {
      const res = await api.post('/api/question/submit', {
        questionId: this.data.current.questionId,
        answer: this.data.userAnswer
      });
      this.setData({ submitted: true, result: res });
    } catch (e) {}
  },

  next() {
    const next = this.data.idx + 1;
    if (next >= this.data.questions.length) {
      wx.showToast({ title: '已经是最后一题', icon: 'none' });
      return;
    }
    this.setData({ idx: next });
    this.showCurrent();
  },

  prev() {
    if (this.data.idx === 0) return;
    this.setData({ idx: this.data.idx - 1 });
    this.showCurrent();
  },

  goWrong() {
    wx.navigateTo({ url: '/pages/question/wrong/wrong' });
  }
});
