// main.cpp

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavcodec/bsf.h>
#include <libavutil/timestamp.h>
}

#include <stdio.h>

int main(int argc, char *argv[]) {
    AVFormatContext *fmt_ctx = NULL;
    AVBSFContext *bsf_ctx = NULL;
    FILE *outfile = NULL;
    int video_stream_index = -1;
    int ret;

    if (argc != 3) {
        fprintf(stderr, "Usage: %s <input.mp4> <output.h264>\n", argv[0]);
        return 1;
    }

    const char *input_file = argv[1];
    const char *output_file = argv[2];

    // 打开输入文件
    if ((ret = avformat_open_input(&fmt_ctx, input_file, NULL, NULL))) {
        fprintf(stderr, "无法打开输入文件\n");
        return ret;
    }

    // 获取流信息
    if ((ret = avformat_find_stream_info(fmt_ctx, NULL)) < 0) {
        fprintf(stderr, "无法获取流信息\n");
        avformat_close_input(&fmt_ctx);
        return ret;
    }

    // 查找视频流
    for (int i = 0; i < fmt_ctx->nb_streams; i++) {
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
            break;
        }
    }
    if (video_stream_index == -1) {
        fprintf(stderr, "未找到视频流\n");
        avformat_close_input(&fmt_ctx);
        return AVERROR(EINVAL);
    }

    // 验证是否为H.264编码
    AVCodecParameters *codecpar = fmt_ctx->streams[video_stream_index]->codecpar;
    if (codecpar->codec_id != AV_CODEC_ID_H264) {
        fprintf(stderr, "视频流不是H.264编码\n");
        avformat_close_input(&fmt_ctx);
        return AVERROR(EINVAL);
    }

    // 初始化bitstream过滤器（AVCC转Annex B）
    const AVBitStreamFilter *bsf = av_bsf_get_by_name("h264_mp4toannexb");
    if (!bsf) {
        fprintf(stderr, "找不到h264_mp4toannexb过滤器\n");
        avformat_close_input(&fmt_ctx);
        return AVERROR(EINVAL);
    }
    if ((ret = av_bsf_alloc(bsf, &bsf_ctx)) < 0) {
        avformat_close_input(&fmt_ctx);
        return ret;
    }
    avcodec_parameters_copy(bsf_ctx->par_in, codecpar);
    if ((ret = av_bsf_init(bsf_ctx)) < 0) {
        av_bsf_free(&bsf_ctx);
        avformat_close_input(&fmt_ctx);
        return ret;
    }

    // 打开输出文件
    outfile = fopen(output_file, "wb");
    if (!outfile) {
        fprintf(stderr, "无法打开输出文件\n");
        av_bsf_free(&bsf_ctx);
        avformat_close_input(&fmt_ctx);
        return AVERROR(EIO);
    }

    AVPacket pkt;
    av_init_packet(&pkt);

    // 读取并处理数据包
    while (av_read_frame(fmt_ctx, &pkt) >= 0) {
        if (pkt.stream_index == video_stream_index) {
            // 发送原始数据包到过滤器
            if ((ret = av_bsf_send_packet(bsf_ctx, &pkt)) < 0) {
                av_packet_unref(&pkt);
                continue;
            }
            av_packet_unref(&pkt);

            // 接收处理后的数据包
            while ((ret = av_bsf_receive_packet(bsf_ctx, &pkt)) == 0) {
                fwrite(pkt.data, 1, pkt.size, outfile);
                av_packet_unref(&pkt);
            }

            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0 && ret != AVERROR_EOF) {
                fprintf(stderr, "过滤器处理错误\n");
                break;
            }
        } else {
            av_packet_unref(&pkt);
        }
    }

    // 冲刷过滤器
    av_bsf_send_packet(bsf_ctx, NULL);
    while (av_bsf_receive_packet(bsf_ctx, &pkt) == 0) {
        fwrite(pkt.data, 1, pkt.size, outfile);
        av_packet_unref(&pkt);
    }

    // 清理资源
    av_bsf_free(&bsf_ctx);
    avformat_close_input(&fmt_ctx);
    fclose(outfile);

    return 0;
}
