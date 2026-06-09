function formatTime(date) {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hour = String(d.getHours()).padStart(2, '0');
  const minute = String(d.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function timeAgo(date) {
  const now = Date.now();
  const past = new Date(date).getTime();
  const diff = Math.floor((now - past) / 1000);
  if (diff < 60) return '刚刚';
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前';
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前';
  if (diff < 604800) return Math.floor(diff / 86400) + '天前';
  return formatTime(date);
}

function statusText(status, type) {
  if (type === 'goods') {
    const map = { 0: '在售', 1: '已售出', 2: '已下架' };
    return map[status] || '未知';
  }
  if (type === 'task') {
    const map = { 0: '待接单', 1: '已接单', 2: '已完成', 3: '已取消' };
    return map[status] || '未知';
  }
  return '未知';
}

function roleText(role) {
  const map = { 0: '普通学生', 1: '社团管理员', 2: '系统管理员' };
  return map[role] || '未知';
}

module.exports = { formatTime, timeAgo, statusText, roleText };
