// 소개글 안에 DescriptionEditor가 심어둔 `![alt](url)` 문법만 실제 <img>로 풀어서 보여준다.
// 범용 마크다운 렌더러가 아니라 이 한 가지 패턴만 인식한다.
export default function DescriptionText({ text, className }) {
  if (!text) return null

  // exec()가 매 호출마다 lastIndex를 옮기는 상태를 갖기 때문에, 모듈 스코프에서 공유하지 않고
  // 렌더링마다 새로 만든다.
  const imagePattern = /!\[([^\]]*)\]\((\S+)\)/g
  const nodes = []
  let lastIndex = 0
  let match
  let key = 0
  while ((match = imagePattern.exec(text))) {
    if (match.index > lastIndex) {
      nodes.push(<span key={key++}>{text.slice(lastIndex, match.index)}</span>)
    }
    const [, alt, url] = match
    nodes.push(
      <img key={key++} src={url} alt={alt} className="max-w-full rounded-lg border border-border my-2 block" />,
    )
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < text.length) {
    nodes.push(<span key={key++}>{text.slice(lastIndex)}</span>)
  }

  return <div className={className}>{nodes}</div>
}
