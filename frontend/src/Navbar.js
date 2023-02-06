import { useNavigate } from 'react-router-dom'
import { useState } from 'react'

import Button from 'react-bootstrap/Button';
import Offcanvas from 'react-bootstrap/Offcanvas';
import Category from "./Category";
import ModalBasic from './Login/ModalBasic';

const options = [
  {
    name: 'Enable backdrop (default)',
    scroll: false,
    backdrop: true,
  },
];

function OffCanvasExample({ name, ...props }) {
  const [show, setShow] = useState(false);

  const handleClose = () => setShow(false);
  const toggleShow = () => setShow((s) => !s);

  return (
    <>
    <div>
      {/* <Button variant="primary" onClick={toggleShow} className="me-2" > */}
      <img className="category_btn1" src="category_btn.png" alt="category_btn1" onClick={toggleShow} />
      {/* </Button> */}
      <Offcanvas className="cate_zone" show={show} onHide={handleClose} {...props}>
        {/* <Offcanvas.Header closeButton> */}
          <Offcanvas.Title><h2 className='cate_title'>지금 뭐해요??</h2></Offcanvas.Title>
        {/* </Offcanvas.Header> */}
        <hr />
        <Category />
      </Offcanvas>
    </div>
    </> 
  );
}

function Example() {
  return (
    <>
      {options.map((props, idx) => (
        <OffCanvasExample key={idx} {...props} />
      ))}
    </>
  );
}



function Navbar() {
  
  let navigate = useNavigate()
  let [inputValue, setInputValue] = useState('')
  console.log(inputValue)

  const [modalOpen, setModalOpen] = useState(false);

  // 모달창 노출
  const showModal = () => {
      setModalOpen(true);
  };

  
  return (
    <div >
      <div className="navbar_up">
        <div onClick={() => navigate('/live')}>
            <img className="App-logo" src="logos/App_logo.png" alt="App_logo"/>
            <span>앱 다운로드</span>
        </div>
        <div onClick={() => navigate('/register')}>고객센터</div>
      </div>
      {/* <hr className="nav_line1"/> */}
      <br />
      <div className="navbar_down">
        <img className="W_logo" src="logos/W_logo.png" alt="W_logo" onClick={() => navigate('/')}/>
        <span><input className="nav_input" type="text" placeholder="검색해보세요" onChange={(e) => setInputValue(e.target.value)} onKeyPress={(e) => { if (e.key === 'Enter') { navigate("/search/" + inputValue) }}} /></span>
        <span>
          <span>
            <div className="nav_btn_box"> 
              <img className="chat_icon" src="chat_icon.png" alt="chat_icon" onClick={() => navigate('/chat')}/>
              <img className="myprofile_icon" src="myprofile_icon.png" alt="myprofile_icon" onClick={() => navigate('/profile')}/>
              <span>
                <img className="logout_icon" src="logout_icon.png" alt="logout_icon" onClick={showModal} />
                {modalOpen && <ModalBasic setModalOpen={setModalOpen} />}
              </span>
            </div>
          </span>
          {/* <br /> */}
          {/* <span>
            <div className="nav_btn_box">
              <img className="login_icon" src="login_icon.png" alt="login_icon" onClick={() => navigate('/login')} />
              <img className="signup_icon" src="signup_icon.png" alt="signup_icon" onClick={() => navigate('/signup')} />
            </div>
          </span> */}
          {/* <br />
          <span>
            <div class="nav_btn_box">
              <a href="https://www.kakaocorp.com/"><img className="login_icon" src="login_icon.png" alt="login_icon" /></a>
              <a href="https://www.coupang.com/"><img className="signup_icon" src="signup_icon.png" alt="signup_icon" /></a>
            </div>
          </span> */}
        </span>
      </div>
      <div>
        <span>
          <Example />
      
          {/* // onClick={() => this.setState({ isCategory: !this.state.isCategory })} */}
          {/* // />
          // <Left primary={this.state.isCategory}>
          //   <Cate>
          //   <Category />
          //   </Cate>
        // </Left> */}
          {/* <button className='sellBtn'>판매하기</button> */}
          <hr className="nav_line2"/>
        </span>
        <img className='sell_icon' src="sell_icon.png" alt="sell_icon" onClick={() => navigate('/register')} />
      </div>
      <br />
    </div>
  )
}

export default Navbar