import { css } from "lit";


const styles = document.styleSheets;
let style;
if (styles.length != 0){
    const { cssRules } = document.styleSheets[0];
    style = css([Object.values(cssRules).map(rule => rule.cssText).join('\n')]);
}else{
    style = css``;
}
export const boostrapStyle = style;
